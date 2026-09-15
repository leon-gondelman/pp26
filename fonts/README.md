# Self-hosted web fonts

Served from this directory so that no visitor request goes to Google. Files
are the latin and latin-ext subsets (Danish æ, ø, å are in latin-ext) as
distributed by Google Fonts on 15 September 2026; `fonts.css` mirrors the
`@font-face` descriptors, so pages render exactly as before.

| Family | Copyright | Licence |
|---|---|---|
| Newsreader | Production Type (Google Fonts project) | SIL OFL 1.1 |
| Source Serif 4 | Adobe Systems Incorporated | SIL OFL 1.1 |
| Inter | The Inter Project Authors (Rasmus Andersson) | SIL OFL 1.1 |
| IBM Plex Mono | IBM Corp. | SIL OFL 1.1 |

`LICENSE-OFL.txt` is the full OFL 1.1 text (as shipped with Inter; the licence
terms are identical for all four). The OFL permits redistribution and web
embedding; it requires that the licence accompany the fonts, which it does.

To regenerate (for example to add a weight), re-run the fetch step recorded in
the git history of this folder, or download the subsets from
https://gwfh.mranftl.com/fonts and replace the files and `fonts.css` together.

Note: the AAU server does not allow `.htaccess` overrides here (a test file
returned HTTP 500), so the woff2 files are served without a Content-Type.
Browsers sniff fonts correctly; nothing to fix on this side.
