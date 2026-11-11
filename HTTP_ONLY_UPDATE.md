# DevForum API — HttpOnly JWT update

Only 7 Java classes are changed/added for the authentication migration:

1. `feature/auth/AuthController.java`
2. `feature/auth/dto/response/AuthResponse.java`
3. `config/JwtAuthenticationFilter.java`
4. `config/SecurityConfig.java`
5. `websocket/WebSocketConfig.java`
6. `websocket/StompAuthChannelInterceptor.java`
7. `websocket/JwtHandshakeInterceptor.java`

The rest of the original API is unchanged.

## Browser authentication

Login/register set:

`access_token=<JWT>; HttpOnly; SameSite=Lax; Path=/`

The JWT is excluded from JSON responses.

The JWT filter accepts the HttpOnly cookie and still accepts an Authorization Bearer header for Postman compatibility.

## CSRF

CSRF is enabled using Spring Security's `CookieCsrfTokenRepository`.

`GET /api/v1/auth/csrf` exposes the token and causes the `XSRF-TOKEN` cookie to be created. The Next.js UI sends it as `X-XSRF-TOKEN` on state-changing requests.

Login/register/logout are ignored by CSRF because they are the authentication lifecycle endpoints.

## Production

For HTTPS production, change the access-token cookie to `.secure(true)` and set the WebSocket/CORS origins to the exact production frontend origin.
