# Static blog engine

Generates the static site for this blog from the files in the parent directory. It's derived from the ERQX blog
engine, with the Play, Akka and git pieces removed.

## Content layout

Jekyll-like: `_config.yml`, `_posts/YYYY-MM-DD-title.(md|html)`, optional `_pages/`, and `_code/` for snippets
referenced from markdown with `@[label](/_code/path/File.scala)`. Posts and pages have an optional YAML header. Anything
not starting with `_` or `.` is copied to the site as is.

## Generating

```
cd _engine
sbt "run .. --url https://jazzy.id.au"
```

This builds the theme assets (less, concatenated css/js) and generates the site into `../_site`.

| Option | Meaning |
|---|---|
| `<base dir>` | Required. The directory containing the blog, ie `..` |
| `--output <dir>` | Where to write the site. **Deleted first.** Default `<base dir>/_site` |
| `--url <url>` | Absolute URL of the host, for the atom feed. Default `http://localhost:8000` |
| `--path-prefix <path>` | Path the site is served from, eg `/blog`, when not at the root of the host. Default none |
| `--assets <dir>` | Built theme assets. Default `_engine/target/web/stage`, as built by sbt |

To preview: `cd _site && python3 -m http.server 8000`.

## Pages generated

* `/` index, `/YYYY.html`, `/YYYY/MM.html`, `/YYYY/MM/DD.html`, `/tags/<tag>.html`, `/tags.html`: lists of post
  summaries (title, date, first 40 words), grouped by the next time unit down, with up/previous/next links.
* `/YYYY/MM/DD/title.html`: the posts, with the same URLs as before.
* `/atom.xml`, `/404.html`, `/_assets/` (theme css, js and icon), and `.nojekyll`.
