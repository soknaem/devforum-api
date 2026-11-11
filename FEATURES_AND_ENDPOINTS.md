# DevForum API — Full Feature & Endpoint Reference

A backend for a Facebook + Stack Overflow style developer community: post resources (books,
code, links, videos, articles) with attached files, follow people, comment (threaded), react,
share, bookmark, get notified in real time, and browse an infinite-scroll feed. Frontend is
expected to be Next.js talking to this API over REST + a WebSocket for live notifications.

## What changed from the previous version

| Area | Before | Now |
|---|---|---|
| JWT secret | Hardcoded string in `application.yml` | Randomly generated on first boot, persisted to `./config/jwt-secret.key` (see `JwtKeyManager`) — never in source control |
| Resources | `url` field only | File uploads (images, PDFs/books, code archives) attached via `Media`, several files per resource |
| Social features | None | Comments (threaded), reactions (6 emotion types + up/downvote), follow/unfollow, bookmarks, shares/reposts |
| Feed | Offset pagination only | Cursor ("infinite scroll") pagination added at `GET /api/v1/feed`, offset pagination kept at `GET /api/v1/resources` |
| Notifications | None | Persisted + pushed live over WebSocket (STOMP) |
| Profile | Basic bio | Avatar/cover upload, location, website, follower/following counts, view history |

## Config you should know about

### JWT secret (random + persisted)
`JwtKeyManager` generates a random 256-bit key on first boot and writes it (base64) to the path in
`jwt.secret-file` (default `./config/jwt-secret.key`). Every later boot reuses that same file, so
tokens keep validating across restarts. **Add `/config/` to `.gitignore`** (already done) — never
commit this file. Delete it if you want to force-rotate the key (this invalidates every
outstanding token).

### File storage
Local disk for now, under `storage.base-path` (default `./uploads`), served back at
`storage.base-url` + `/files/**`. Swap `LocalDiskStorageService` for an S3/GCS-backed
`StorageService` later — no controller changes needed.

### Database
`application.yml` is set up for Oracle as you specified (`ddl-auto: create-drop`). PostgreSQL and
MySQL blocks are still there, commented out, if you want to switch back.

### CORS
`cors.allowed-origins` (default `http://localhost:3000`) is wired for your Next.js frontend.

### API docs
Springdoc/Swagger is on the classpath — once the app is running, visit
`http://localhost:8085/swagger-ui.html`.

---

## Full endpoint list

Base path for everything below: `http://localhost:8085`. Auth column: 🔓 public, 🔒 requires
`Authorization: Bearer <token>`, 🔒* owner or admin only.

### Auth
| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/api/v1/auth/register` | 🔓 | `{ username, email, password, fullName }` |
| POST | `/api/v1/auth/login` | 🔓 | `{ username, password }` |

### Resources
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/v1/resources` | 🔒 | JSON create (no files). Body: `{ title, description, url, type, status, categoryId, tags[] }` |
| POST | `/api/v1/resources/with-media` | 🔒 | **multipart/form-data**: `title, description, url, type, status, categoryId, tags (comma-separated), files[]` — create + upload in one call |
| GET | `/api/v1/resources/{id}` | 🔓 | Increments view count; records history if authenticated |
| GET | `/api/v1/resources` | 🔓 | Query: `search, categoryId, type, status, tag, page, size, sortBy, sortDir` (offset pagination) |
| PUT | `/api/v1/resources/{id}` | 🔒* | Same body shape as create |
| DELETE | `/api/v1/resources/{id}` | 🔒* | |
| POST | `/api/v1/resources/{id}/share` | 🔒 | Body: `{ comment }` (optional). Creates a repost pointing at the original |

### Media (file uploads)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/v1/media/upload` | 🔒 | multipart `file` + `folder` (e.g. `misc`) — standalone upload |
| POST | `/api/v1/media/resources/{resourceId}` | 🔒* | multipart `file` — attach to an existing resource |
| GET | `/api/v1/media/resources/{resourceId}` | 🔓 | List all media attached to a resource |
| DELETE | `/api/v1/media/{mediaId}` | 🔒* | |

Allowed extensions / size cap are in `storage.allowed-extensions` / `storage.max-file-size-mb`.

### Comments (threaded, one level deep)
| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/api/v1/resources/{resourceId}/comments` | 🔒 | `{ content, parentId? }` |
| GET | `/api/v1/resources/{resourceId}/comments` | 🔓 | Query: `page, size` — top-level comments only |
| GET | `/api/v1/comments/{commentId}/replies` | 🔓 | Query: `page, size` |
| PUT | `/api/v1/comments/{commentId}` | 🔒* | `{ content }` |
| DELETE | `/api/v1/comments/{commentId}` | 🔒* | |

### Reactions (like/love/haha/wow/sad/angry/upvote/downvote)
| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/api/v1/reactions` | 🔒 | `{ targetType: RESOURCE\|COMMENT, targetId, type }` — upserts |
| DELETE | `/api/v1/reactions/{targetType}/{targetId}` | 🔒 | Removes the caller's reaction |
| GET | `/api/v1/reactions/{targetType}/{targetId}/summary` | 🔓 | Counts per type + caller's own reaction if authenticated |

### Follow
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/v1/users/{username}/follow` | 🔒 | |
| DELETE | `/api/v1/users/{username}/follow` | 🔒 | |
| GET | `/api/v1/users/{username}/followers` | 🔓 | Query: `page, size` |
| GET | `/api/v1/users/{username}/following` | 🔓 | Query: `page, size` |

### Bookmarks (save for later)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/v1/resources/{resourceId}/bookmark` | 🔒 | |
| DELETE | `/api/v1/resources/{resourceId}/bookmark` | 🔒 | |
| GET | `/api/v1/users/me/bookmarks` | 🔒 | Query: `page, size` |

### User profile & history
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/v1/users/me` | 🔒 | Your own profile |
| PUT | `/api/v1/users/me/profile` | 🔒 | `{ fullName, bio, location, website }` |
| POST | `/api/v1/users/me/avatar` | 🔒 | multipart `file` |
| POST | `/api/v1/users/me/cover` | 🔒 | multipart `file` |
| GET | `/api/v1/users/me/history` | 🔒 | Recently viewed resources, query: `page, size` |
| GET | `/api/v1/users/{username}` | 🔓 | Public profile |
| GET | `/api/v1/users/{username}/resources` | 🔓 | Query: `page, size` |

### Feed (infinite scroll)
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/v1/feed` | 🔓 | Query: `cursor` (last item's id from previous page, omit for page 1), `limit` (default 20) |

Response shape:
```json
{ "content": [ ... ], "nextCursor": 042, "hasMore": true }
```
Pass `nextCursor` back as `cursor` to fetch the next batch. `hasMore: false` means you've reached
the end.

### Notifications (persisted + real-time)
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/v1/notifications` | 🔒 | Query: `page, size` |
| GET | `/api/v1/notifications/unread-count` | 🔒 | |
| PATCH | `/api/v1/notifications/{id}/read` | 🔒 | |
| PATCH | `/api/v1/notifications/read-all` | 🔒 | |

**Live push (WebSocket/STOMP):**
1. Connect: `new SockJS('http://localhost:8085/ws')` → `Stomp.over(socket)`
2. On `stompClient.connect`, pass a native header: `Authorization: Bearer <token>`
3. Subscribe: `stompClient.subscribe('/user/queue/notifications', callback)`

You'll receive a `NotificationResponse` JSON payload the instant a follow/comment/reply/reaction/
share happens to you — no polling needed. `GET /api/v1/notifications` is still there for
loading history/on page refresh.

---

## Data model additions

- **Media** — a file (image/PDF/archive/other) attached to a Resource, or standalone (avatar/cover). Tracks `storageKey`, `url`, `contentType`, `sizeBytes`, `mediaType`.
- **Comment** — belongs to a Resource, optional `parent` for one level of threaded replies.
- **Reaction** — polymorphic via `targetType` (`RESOURCE`/`COMMENT`) + `targetId`; one per user per target.
- **Follow** — `follower` → `following` edge, unique per pair.
- **Bookmark** — `user` saved `resource`, unique per pair.
- **ResourceView** — last-viewed timestamp per (user, resource); powers `/users/me/history`.
- **Notification** — `recipient`, `actor`, `type`, `targetType/targetId`, `message`, `isRead`.
- **Resource.sharedFrom** — self-referencing FK; a share is just a new Resource row pointing at the original, so it appears naturally in feeds/profiles.

## Known trade-offs (given the scope of this pass)

- Comment/reaction counts on `ResourceResponse` are computed with a couple of extra queries per
  resource — fine at demo scale, but for a high-traffic feed you'd want to denormalize these
  counters onto the `Resource` row (or cache them) instead of counting on every read.
- Threading is one level deep (comment → replies), matching most forum UIs; deeper nesting would
  need a recursive/self-referencing query.
- The WebSocket auth interceptor reads the JWT off the STOMP `CONNECT` frame's native headers,
  since browsers can't attach custom HTTP headers to the initial WS handshake — make sure your
  Next.js STOMP client sets `Authorization` (or `token`) as a **connect header**, not a URL param.
- `net.datafaker` and `org.modelmapper` are on the classpath per your build.gradle; only datafaker
  is used so far (in `DataInitializer`, to seed realistic demo content). ModelMapper isn't wired
  in yet since MapStruct already covers entity↔DTO mapping — happy to use it somewhere specific if
  you have a use case in mind.
- I removed four `testImplementation` lines from your build.gradle
  (`spring-boot-starter-actuator-test`, `spring-boot-starter-data-jpa-test`,
  `spring-boot-starter-validation-test`, `spring-boot-starter-webmvc-test`) — those artifact
  coordinates don't exist in Maven Central and would fail dependency resolution. Everything else
  from your file was kept as-is.
