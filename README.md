# pressure-integrity-test

A tool to monitor pressure integrity tests and extrapolate leakage.

## Download

- **Windows:** [Download the latest release](https://github.com/OWNER/REPO/releases/latest) (zip with app and bundled JRE). Replace `OWNER/REPO` with this repo (e.g. `lajthabalazs/pressure-integrity-test`).

## GitHub Pages

The project uses GitHub Actions to deploy a small site to GitHub Pages and to publish the Windows zip on each release.

1. **Enable GitHub Pages:** In the repo go to **Settings → Pages**. Under "Build and deployment", set **Source** to **GitHub Actions**.
2. **Releases:** Pushing a tag like `v1.0` runs the build, creates a GitHub Release, and attaches the Windows zip. The Pages site links to the latest release for download.
3. **Pages only:** Pushing to `main` (or running the "GitHub Pages" workflow) deploys the site without building; the download link points to the Releases page.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](LICENSE) in this repository for the full text, or [https://opensource.org/license/gpl-3-0](https://opensource.org/license/gpl-3-0) for an overview.
