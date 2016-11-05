(function() {
    'use strict';

    angular
        .module('splitItApp')
        .controller('TransactionDetailController', TransactionDetailController);

    TransactionDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Transaction', 'User'];

    function TransactionDetailController($scope, $rootScope, $stateParams, previousState, entity, Transaction, User) {
        var vm = this;

        vm.transaction = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('splitItApp:transactionUpdate', function(event, result) {
            vm.transaction = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
