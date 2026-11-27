import {Routes} from '@angular/router';
import {DossierCreate} from './components/dossier-create/dossier-create';
import {DossierWorkspaceComponent} from './components/dossier-workspace/dossier-workspace.component';
import {ForbiddenComponent} from './components/forbidden/forbidden.component';
import {HomeComponent} from './components/home/home.component';
import {SectionComposant} from './components/section/section.composant';
import {UserProfileComponent} from './components/user-profile/user-profile.component';
import {canActivateRequiredRole} from './guards/auth-role.guard';

export const appRoutes: Routes = [
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    component: HomeComponent,
  },
  {
    path: 'forbidden',
    component: ForbiddenComponent,
  },
  {
    path: 'profile',
    component: UserProfileComponent,
    canActivate: [canActivateRequiredRole],
    data: {role: 'ETUDIANT'}
  },
  {
    path: 'ues',
    component: SectionComposant,
    canActivate: [canActivateRequiredRole],
    data: {role: 'ETUDIANT'}
  },
  {
    path: 'createDossier',
    component: DossierCreate,
    canActivate: [canActivateRequiredRole],
    data: {role: 'ETUDIANT'}
  },
  {
    path: 'dossier/:id',
    component: DossierWorkspaceComponent,
    canActivate: [canActivateRequiredRole],
    data: {role: 'ETUDIANT'}
  }
];
