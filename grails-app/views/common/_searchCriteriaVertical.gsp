<div id="searchCriteria"  style="border-right: 0px solid lightgrey; padding: 0px;">
	<script>
		$(function() {
			$("#categoryFilter").change(function () { 
				$(this).closest("form").submit();
			});
		});
	</script>
	<div>
	
		<fieldset style="padding: 10px">
			<table>
				<tr class="">
					<td colspan="2">				
						<label>Search</label>
					</td>
				</tr>
				<tr>
					<td colspan="2" style="text-align: right;">
						<g:render template="../inventoryItem/searchInventory"/>
					</td>
				</tr>
			</table>
			
		
			<table>
				<tr class="">
					<td>				
						<label>Filters </label>
					</td>
					<td class="right">
						<g:if test="${categoryFilters.size() >= 2 }">
							<g:link action="clearCategoryFilters">
								<img src="${createLinkTo(dir: 'images/icons/silk', file: 'bin.png' )}" style="vertical-align:middle"/>
							</g:link>							
						</g:if>
					</td>				
				</tr>
			
				<g:each var="categoryFilter" in="${categoryFilters }" status="status">
					<tr class="${status%2?'even':'odd' }">
						<td style="text-align: left;">									
							${categoryFilter?.name }
						</td>
						<td style="text-align: right">
							<g:link action="removeCategoryFilter" params="[categoryId:categoryFilter.id]">
								<img src="${createLinkTo(dir: 'images/icons/silk', file: 'cross.png' )}" style="vertical-align:middle"/>
							</g:link>
						</td>
					</tr>
				</g:each>
				<tr>
					<td colspan="2">
						<g:form action="addCategoryFilter">
							<select id="categoryFilter" name="categoryId" >
								<option value="">Add a new filter</option>
								<g:render template="../category/selectOptions" model="[category:rootCategory, selected:null, level: 0]"/>								
							</select>										
						</g:form>
					</td>
				</tr>
				<%-- 
				<tr class="prop">
					<td colspan="2">				
						<label>Attributes</label>
					</td>
				</tr>
				<tr>
					<td>
						<table>
							<g:each var="attributeFilter" in="${commandInstance?.attributeMap }" status="status">
								<tr class="prop ${status%2?'even':'odd' }">				
									<td>${attributeFilter }</td>
								</tr>
							</g:each>
						</table>				
					
					</td>
				</tr>
				--%>
				<%-- 
				<g:each var="attribute" in="${org.pih.warehouse.product.Attribute.list()}" status="status">
					<tr class="prop">
						<td colspan="2">
							<label>${attribute.name }</label>	
						</td>
					</tr>
					<tr>
						<td colspan="2">
							<table>
								<g:if test="${attribute.options }">
									<tr>
										<g:each var="attributeOption" in="${attribute?.options }" status="status2">
											<tr class="${status2%2?'even':'odd' }">
												<td style="text-align: left;">									
													<a href="${createLink(action:'addAttributeFilter',params:["attributeId":attribute.id,value:attributeOption])}">${(attributeOption)?:'none' }</a>
												</td>
												<td style="text-align: right">
													<g:link action="removeAttributeFilter" params="[attributeId:attribute.id,value:attributeOption]">
														<img src="${createLinkTo(dir: 'images/icons/silk', file: 'cross.png' )}" style="vertical-align:middle"/>
													</g:link>
												</td>
											</tr>
												
												
											</li>
										</g:each>
									</tr>
									
								</g:if>
							</table>
						</td>
					</tr>
				</g:each>	
				--%>
				
			</table>
		</fieldset>
		<span class="fade">Showing ${productInstanceList?.size() } product(s)</span>			
	</div>
</div>						