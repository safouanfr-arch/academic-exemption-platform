import {CommonModule} from '@angular/common';
import {Component, effect, inject, untracked, ViewChild} from '@angular/core';
import {RouterModule} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {MatSidenav, MatSidenavModule} from '@angular/material/sidenav';
import {MatToolbarModule} from '@angular/material/toolbar';
import {AuthService} from '../../auth/auth.service';
import {MenuComponent} from '../menu/menu.component';
import {SidebarService} from './sidebar.service';

@Component({
  selector: 'app-sidebar-cmp',
  imports: [
    RouterModule,
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatIconModule,
    MatListModule,
    MenuComponent,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);
  readonly sidebarService = inject(SidebarService);

  protected readonly authenticated = this.authService.getAuthenticationState();

  @ViewChild(MatSidenav) drawer!: MatSidenav;

  constructor() {
    effect(() => {
      this.sidebarService.toggleCmd();

      // On lit le signal, puis on declenche vraiment le toggle sans
      // reinjecter le drawer dans la reactvite du signal.
      untracked(() => {
        this.drawer?.toggle();
      });
    });
  }
}
