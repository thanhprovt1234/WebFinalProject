# H2T editorial UI

The homepage now uses a full-width fashion campaign, direct category navigation,
expandable styling notes, a brand story, and account registration. Category cards
are editorial collection previews; they are not a connected product catalog.
Existing authentication actions, CSRF protection and validation are preserved.

Shared styling: `src/main/webapp/css/fashion-theme.css`, loaded after `app.css`.
The previous `glass-theme.css` is no longer loaded.
Bootstrap 5.3.3 and Bootstrap Icons 1.11.3 are served from the local `css/vendor`
and `js/vendor` directories so rendering and the mobile menu do not depend on a CDN.

## Verification

- Maven WAR packaging succeeded with Java 26 (tests skipped for this presentation change).
- Live JSP requests for home, login, registration and password recovery returned HTTP 200.
- Headless Chrome at 1440px and 390px: no horizontal overflow or broken images.
- Mobile navigation expanded, category details opened, and password visibility toggled.
- Authentication form submissions were not exercised; existing server actions were retained.

## Image asset

- File: `src/main/webapp/images/h2t-editorial-hero.png`
- Created with the built-in imagegen tool.
- Generated campaign artwork, not photography of actual inventory.
- Final prompt:

> Create a photorealistic-natural premium Vietnamese menswear fashion campaign photograph for a website hero, very wide landscape 2.4:1. Two adult Vietnamese male fashion models age 25, right half of composition, one wearing relaxed ivory short sleeve shirt and dark trousers, other wearing black polo with ivory collar and charcoal trousers. Full upper bodies and thighs visible, natural confident editorial poses, textured dark charcoal concrete urban wall and subtle blurred pedestrian motion at edges, muted warm monochrome film photography, soft daylight, luxurious real fabric detail. Left 45 percent is mostly empty dark textured wall for overlay website typography. No text, no logos, no watermarks. This is a photographic asset, not a screenshot or UI.
