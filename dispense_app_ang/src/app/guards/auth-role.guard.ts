import {inject} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {AuthGuardData, createAuthGuard} from 'keycloak-angular';

const checkRequiredRoleAccess = async (
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  authData: AuthGuardData
): Promise<boolean | UrlTree> => {
  const requiredRoleName = route.data['role'];

  if (!requiredRoleName) {
    return false;
  }

  // Keycloak peut donner des roles "realm" ou "resource".
  const hasRealmRole = authData.grantedRoles.realmRoles?.includes(requiredRoleName) ?? false;
  const hasResourceRole = Object.values(authData.grantedRoles.resourceRoles ?? {})
    .some(roleList => roleList.includes(requiredRoleName));

  if (authData.authenticated && (hasRealmRole || hasResourceRole)) {
    return true;
  }

  return inject(Router).parseUrl('/forbidden');
};

export const canActivateRequiredRole = createAuthGuard<CanActivateFn>(checkRequiredRoleAccess);
