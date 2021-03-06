(function() {
    'use strict';

    angular
        .module('splitItApp')
        .controller('TransactionByGroupDialogController', TransactionByGroupDialogController);

    TransactionByGroupDialogController.$inject = ['$scope', '$state','$stateParams', '$uibModalInstance', 'Summary', 'ParseLinks', 'AlertService',  'paginationConstants','Principal', 'Auth'];

    function TransactionByGroupDialogController ($scope, $state, $stateParams, $uibModalInstance, Summary, ParseLinks, AlertService, paginationConstants, Principal, Auth) {
        var vm = this;

        var copyAccount = function (account) {
            return {
                activated: account.activated,
                email: account.email,
                firstName: account.firstName,
                langKey: account.langKey,
                lastName: account.lastName,
                login: account.login,
                id: account.id,
                authorities: account.authorities
            };
        };

         vm.loggedInAccount = null;

         var promise = new Promise(
         function(resolve, reject) {

            Principal.identity().then(function(account) {
                vm.loggedInAccount = copyAccount(account);
            });

             resolve();
          }
          );

        promise.then(function(){
            loadAll();
        });

        function loadAll () {
            Summary.groupSummaries.query({
                groupId: $stateParams.id,
                login : vm.loggedInAccount.login
            }, onSuccess, onError);

            function sort() {
                var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
                if (vm.predicate !== 'id') {
                    result.push('id');
                }
                return result;
            }
            function onSuccess(data, headers) {
                vm.totalItems = data.summaries.length;
                vm.queryCount = vm.totalItems;
                vm.data = data;
                debugger;
            }

            function onError(error) {
            alert(error.data.message);
                AlertService.error(error.data.message);
            }
        }

        vm.clear = clear;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

    }
})();
