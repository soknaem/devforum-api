# DevForum API HttpOnly authentication update

Replace the corresponding files in your existing project.

This update:
- stores JWT in an HttpOnly `access_token` cookie;
- never serializes the JWT in `AuthResponse`;
- reads JWT from the cookie (while keeping Authorization: Bearer compatibility);
- adds `/api/v1/auth/logout`;
- adds `/api/v1/auth/csrf`;
- enables CSRF protection using `XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header;
- restricts WebSocket origins to `cors.allowed-origins`.

For production HTTPS, change the access-token cookie `secure(false)` to `secure(true)`.
