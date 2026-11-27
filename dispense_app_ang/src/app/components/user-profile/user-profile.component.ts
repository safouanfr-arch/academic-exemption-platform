import {Component, inject} from '@angular/core';
import {AuthService} from '../../auth/auth.service';

@Component({
  selector: 'app-user-profile',
  templateUrl: 'user-profile.component.html',
  styleUrls: ['user-profile.component.css']
})
export class UserProfileComponent {
  private readonly authService = inject(AuthService);

  // Ce composant lit simplement le profil deja synchronise par AuthService.
  protected readonly appUser = this.authService.getAppUser();
  protected readonly profileLoading = this.authService.getAppUserLoading();
  protected readonly profileError = this.authService.getAppUserError();
}
