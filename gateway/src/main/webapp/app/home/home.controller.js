(function() {
    'use strict';

    angular
        .module('gatewayApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state','$stateParams','AuthServerProvider'];

    function HomeController ($scope, Principal, LoginService, $state,$stateParams,AuthServerProvider) {
    	
    	if (($stateParams.access_token != null)&&($stateParams.expires_in != null)){
    		AuthServerProvider.storeAuthenticationToken($stateParams.access_token);
    		Principal.identity(true).then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
            });
    	}
    	
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        vm.register = register;
        $scope.$on('authenticationSuccess', function() {
            getAccount();
        });

        getAccount();

        function getAccount() {
            Principal.identity().then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
            });
        }
        function register () {
            $state.go('register');
        }
    }
})();
