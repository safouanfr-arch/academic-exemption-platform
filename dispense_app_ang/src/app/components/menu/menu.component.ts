import {Component, inject} from '@angular/core';
import {RouterModule} from '@angular/router';
import {MatButtonModule, MatIconButton} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';
import {AuthService} from '../../auth/auth.service';
import {DossierStat} from '../dossier-stat/dossier-stat';
import {SidebarService} from '../sidebar/sidebar.service';

@Component({
  selector: 'app-menu',
  imports: [RouterModule, MatIconModule, DossierStat, MatIconButton, MatTooltip, MatButtonModule],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
})
export class MenuComponent {
  private readonly authService = inject(AuthService);
  private readonly sidebarService = inject(SidebarService);

  protected readonly authenticated = this.authService.getAuthenticationState();

  login() {
    this.authService.login();
  }

  logout() {
    this.authService.logout();
  }

  getCurrentUsername() {
    return this.authService.getCurrentUsername();
  }

  toggleSidebar() {
    this.sidebarService.toggleMenu();
  }
}
