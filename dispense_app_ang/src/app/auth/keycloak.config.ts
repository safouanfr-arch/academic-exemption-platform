import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {
  AutoRefreshTokenService,
  createInterceptorCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  IncludeBearerTokenCondition,
  includeBearerTokenInterceptor,
  provideKeycloak,
  UserActivityService,
  withAutoRefreshToken,
} from 'keycloak-angular';
import {environment} from '../../environments/environment';

const resourceServerTokenCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: new RegExp(
    `^(${environment.scheme}:\\/\\/${environment.resourceServer.host}:${environment.resourceServer.port})(\\/.*)?$`,
    'i'
  ),
  bearerPrefix: 'Bearer'
});

export const provideAppKeycloak = () =>
  provideKeycloak({
    config: {
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    },
    initOptions: {
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      redirectUri: `${window.location.origin}/`,
      scope: 'openid email',
    },
    features: [
      withAutoRefreshToken({
        onInactivityTimeout: 'logout',
        sessionTimeout: 600000
      })
    ],
    providers: [
      provideHttpClient(withInterceptors([includeBearerTokenInterceptor])),
      {
        provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
        // Le jeton Bearer ne part que vers notre serveur.
        useValue: [resourceServerTokenCondition]
      },
      AutoRefreshTokenService,
      UserActivityService
    ]
  });
