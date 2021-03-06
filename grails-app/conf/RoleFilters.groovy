/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 * */
class RoleFilters {
    def userService
    def dependsOn = [SecurityFilters]
    def static changeActions = ['edit', 'delete', 'create', 'add', 'process', 'save',
            'update', 'importData', 'receive', 'showRecordInventory', 'withdraw', 'cancel', 'change', 'toggle']
    def static changeControllers = ['createProductFromTemplate']
    def static adminControllers = ['createProduct', 'createProductFromTemplate', 'admin']
    def static adminActions = ['console':['index','execute'], 'product': ['create'], 'person': ['list'], 'user': ['list'], 'location': ['edit'], 'shipper': ['create'], 'locationGroup': ['create'], 'locationType': ['create'], '*': ['delete']]
    def filters = {
        readonlyCheck(controller: '*', action: '*') {
            before = {
                if (SecurityFilters.actionsWithAuthUserNotRequired.contains(actionName) || actionName == "chooseLocation" || controllerName == "errors") return true
                def missBrowser = !userService.canUserBrowse(session.user)
                def missManager = needManager(controllerName, actionName) && !userService.isUserManager(session.user)
                def missAdmin = needAdmin(controllerName, actionName) && !userService.isUserAdmin(session.user)
                if (missBrowser || missManager || missAdmin) {
                    response.sendError(401)
                    return false
                }
                return true
            }
        }
    }

    def static Boolean needAdmin(controllerName, actionName) {
        adminControllers?.contains(controllerName) || adminActions[controllerName]?.contains(actionName) || adminActions['*'].any { actionName?.startsWith(it) }
    }

    def static Boolean needManager(controllerName, actionName) {
        changeActions.any { actionName?.startsWith(it) } || controllerName?.contains("Workflow") || changeControllers?.contains(controllerName)
    }


}
