package org.pih.warehouse.report;

import java.util.Map;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.grails.plugins.excelimport.ExcelImportUtils;
import org.pih.warehouse.inventory.Transaction;
import org.pih.warehouse.inventory.InventoryItem;
import org.pih.warehouse.core.Location;
import org.pih.warehouse.product.Product;
import org.pih.warehouse.product.Category;
import org.pih.warehouse.product.ProductAttribute;
import org.pih.warehouse.shipping.Shipment;
import org.pih.warehouse.core.Constants 
import org.pih.warehouse.core.Location
import org.pih.warehouse.core.LocationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.Errors;
import org.xhtmlrenderer.pdf.ITextRenderer;

import org.pih.warehouse.reporting.Consumption;

class ReportService implements ApplicationContextAware {
	
	def sessionFactory
	def productService
	def inventoryService
	def shipmentService
	def localizationService
	
	ApplicationContext applicationContext
	
	boolean transactional = true
	
	
	public void generateChecklistReport(ChecklistReportCommand command) {
		command.shipment.shipmentItems.each { shipmentItem -> 
			command.checklistReportEntryList << new ChecklistReportEntryCommand(shipmentItem: shipmentItem)
		}
	
	}
	
	
	public void generateProductReport(ProductReportCommand command) { 
		
		command.inventoryItems = InventoryItem.findAllByProduct(command?.product)
		command.quantityInitial = inventoryService.getInitialQuantity(command?.product, command?.location, command?.startDate)

		def transactionEntries = inventoryService.getTransactionEntries(command?.product, command?.location, command?.startDate, command?.endDate)
				
		// Calculate quantity at each transaction entry point.
		def quantity = command?.quantityInitial;
		transactionEntries.each { transactionEntry ->
			//log.info(transactionEntry)
			def productReportEntry = new ProductReportEntryCommand(transactionEntry: transactionEntry, balance: 0)
			productReportEntry.balance = inventoryService.adjustQuantity(quantity, transactionEntry)
			command.productReportEntryList << productReportEntry
			
			// Need to keep track of the running total so we can adjust the balance as we go
			quantity = productReportEntry.balance
		}
		//command.quantityFinal = quantity;
		command.quantityFinal = inventoryService.getCurrentQuantity(command?.product, command?.location, command?.endDate)

	}
	
	
	/**
	 * 
	 * @param command
	 */
	public void generateTransactionReport(InventoryReportCommand command) { 
		
		def categories = []
		categories << command.category
		
		// Initialize the report map to reference all products to be displayed
		def products = inventoryService.getProductsByCategories(categories)
		products.each { product ->
			def entry = command.inventoryReportEntryMap[product];
			if (!entry) entry = new InventoryReportEntryCommand(product: product);
			entry.quantityInitial = inventoryService.getInitialQuantity(product, command?.location, command?.startDate)
			entry.quantityFinal = inventoryService.getCurrentQuantity(product, command?.location, command?.endDate);
			command.inventoryReportEntryMap[product] = entry;
		}
		
		// TODO Need to restrict by date and category 
		def transactionEntries = inventoryService.getTransactionEntriesByInventory(command.location.inventory);
		
		// Iterate over the transaction entries for the given time period to tabulate totals.
		// Each time we encounter an INVENTORY, compare that quantity with the running total,
		// and add / subract to "adjustment" as appropriate.  Then set the running
		// total to the new inventory and continue with the running total...
		transactionEntries.each {
			def product = it?.inventoryItem?.product
			def transactionType = it?.transaction?.transactionType

			// TODO Move this filtering to the getTransactionEntries() service call above
			// Filter by category, location, startDate, endDate (should move this to the service layer)
			if ((!command.category || command.category == product.category) &&
				(!command.location || command.location.inventory == it.transaction.inventory) &&
				(!command.startDate || it.transaction?.transactionDate?.after(command.startDate)) &&
				(!command.endDate || it.transaction?.transactionDate?.before(command.endDate))) {
	
				def entry = command.inventoryReportEntryMap[product]
				if (!entry) {
					entry = new InventoryReportEntryCommand(product: product);
					command.inventoryReportEntryMap[product] = entry
				}
				
				if (transactionType?.id == Constants.CONSUMPTION_TRANSACTION_TYPE_ID) {
					entry.quantityRunning += it.quantity
					entry.quantityConsumed += it.quantity
					entry.quantityTotalOut += it.quantity
				}
				else if (transactionType?.id == Constants.ADJUSTMENT_CREDIT_TRANSACTION_TYPE_ID) {
					entry.quantityRunning += it.quantity
					entry.quantityFound += it.quantity
					entry.quantityAdjusted += it.quantity
					//entry.quantityTotalIn += it.quantity
				}
				else if (transactionType?.id == Constants.EXPIRATION_TRANSACTION_TYPE_ID) {
					entry.quantityRunning -= it.quantity
					entry.quantityExpired += it.quantity
					entry.quantityTotalOut += it.quantity
				}
				else if (transactionType?.id == Constants.DAMAGE_TRANSACTION_TYPE_ID) {
					entry.quantityRunning -= it.quantity
					entry.quantityDamaged += it.quantity
					entry.quantityTotalOut += it.quantity
				}
				else if (transactionType?.id == Constants.TRANSFER_IN_TRANSACTION_TYPE_ID) {
					entry.quantityRunning += it.quantity
					entry.quantityTransferredIn += it.quantity
					entry.quantityTotalIn += it.quantity
					if (!entry.quantityTransferredInByLocation[it.transaction.source]) { 
						entry.quantityTransferredInByLocation[it.transaction.source] = 0
					}
					entry.quantityTransferredInByLocation[it.transaction.source] += it.quantity					
				}
				else if (transactionType?.id == Constants.TRANSFER_OUT_TRANSACTION_TYPE_ID) {
					entry.quantityRunning -= it.quantity
					entry.quantityTransferredOut += it.quantity
					entry.quantityTotalOut += it.quantity
					if (!entry.quantityTransferredOutByLocation[it.transaction.destination]) { 
						entry.quantityTransferredOutByLocation[it.transaction.destination] = 0;
					}
					entry.quantityTransferredOutByLocation[it.transaction.destination] += it.quantity					
				}
				else if (transactionType?.id == Constants.ADJUSTMENT_DEBIT_TRANSACTION_TYPE_ID) {
					entry.quantityRunning -= it.quantity
					entry.quantityLost += it.quantity
					entry.quantityAdjusted -= it.quantity
					entry.quantityTotalOut += it.quantity
				}
				else if (transactionType?.id == Constants.INVENTORY_TRANSACTION_TYPE_ID) {
					def diff = it.quantity - entry.quantityRunning
					entry.quantityAdjusted += diff					
					entry.quantityRunning = it.quantity;
					if (diff > 0)
						entry.quantityFound += diff;
					else 
						entry.quantityLost += diff	
				}
				else if (transactionType?.id == Constants.PRODUCT_INVENTORY_TRANSACTION_TYPE_ID) {
					def diff = it.quantity - entry.quantityRunning
					entry.quantityAdjusted += diff
					entry.quantityRunning = it.quantity;
					if (diff > 0)
						entry.quantityFound += diff;
					else 
						entry.quantityLost += diff	
				}
			}
		}		
	}	
	
	/**
	 * Generate a PDF of the page at the given URL and write to the given output stream.
	 * 
	 * @param url
	 * @param outputStream
	 * @return
	 */
	private generatePdf(url, outputStream) {
		log.info "Generate pdf from page at URL " + url
		//ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocument(url);
		//renderer.getSharedContext().setUserAgentCallback(new MyUserAgent());
		renderer.layout();
		renderer.createPDF(outputStream);
		//byte [] b = baos.toByteArray();
		//baos.close();
		//log.info(b)
		//return b;
	}

	
}