# Deploy from a phone (no laptop)

You only need a phone browser, a free **GitHub**, **MongoDB Atlas** and **Render** account.

## 1. Atlas (database)
Follow **README → 3A**. Keep the connection string handy (with `/blossom_erp` before the `?`).

## 2. Get the project into GitHub (phone-friendly trick)
Mobile browsers cannot upload folders, so let GitHub unzip it for you:

1. GitHub → **New repository** (private is fine), e.g. `blossom-erp`. Tick *Add a README* so it is not empty.
2. In the repo: **Add file → Upload files** → upload **`blossom-erp.zip`** (the ZIP you downloaded; keep its name). Commit.
3. **Add file → Create new file**. Name it exactly `.github/workflows/unzip.yml` and paste:

```yaml
name: Unpack project zip
on: workflow_dispatch
permissions:
  contents: write
jobs:
  unpack:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Unzip and commit
        run: |
          set -e
          unzip -q -o blossom-erp.zip -d _unz -x "*/.github/*"
          shopt -s dotglob
          mv _unz/*/* .
          rm -rf _unz blossom-erp.zip
          git config user.name "github-actions"
          git config user.email "actions@github.com"
          git add -A
          git commit -m "Unpack Blossom ERP project"
          git push
```
4. Open the **Actions** tab → *Unpack project zip* → **Run workflow**. After ~30 s the repo root contains `pom.xml`, `Dockerfile`, `render.yaml`, `src/` …
   (If the repo's default branch protections or Actions permissions block the push: Settings → Actions → General → *Workflow permissions* → **Read and write**.)

## 3. Render
Follow **README → 3B**: New + → Blueprint → pick the repo → paste `MONGODB_URI` → Apply.
Open the generated URL, sign in with `admin@blossom.demo` / `demo1234`.

## Troubleshooting
| Symptom | Fix |
|---|---|
| Render log: `JWT_SECRET is too short` or warning `JWT_SECRET is not set` | set `JWT_SECRET` to 32+ characters (the Blueprint does this automatically) |
| Login page says `Database: DOWN` | Atlas **Network Access** must allow `0.0.0.0/0`; check user/password and that the URI contains `/blossom_erp` |
| `bad auth` in logs | wrong Atlas password, or special characters not URL-encoded |
| Site is slow on first visit | free Render instance was asleep; wait ~1 min |
| No demo accounts / old demo data still showing | `SEED_DEMO_DATA` must be `true` **and** the database must have no employees yet. If old data is already there, add `RESET_DEMO_DATA=true` once (⚠️ wipes the database), redeploy, then delete that variable |
