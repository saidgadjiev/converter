#!/usr/bin/env python
# coding: utf-8
# jpeg2epub: copyright (C) 2013, RUAMEL bvba, A. van der Neut

import os
import sys
from io import open
from textwrap import dedent
from cStringIO import StringIO
import zipfile
import uuid
import datetime


class Jpeg2Epub(object):
    """simple epub creator for series of JPEG image files

    creates the file epub file in memory
    """
    version = 1  # class version when used a s library

    def __init__(self, title, file_name=None, creator=None, title_sort=None,
                 series=None, series_idx=None, verbose=0):
        self._output_name = file_name if file_name else \
            title.replace(' ', '_') + '.epub'
        self._files = None
        self._zip = None  # the in memory zip file
        self._zip_data = None
        self._content = []
        self._count = 0
        self._series = series
        self._series_idx = series_idx
        self.d = dict(
            title=title,
            title_sort=title_sort if title_sort else title,
            creator=creator if creator else 'Unknown',
            opf_name="c.opf",
            toc_name="toc.ncx",
            ncx_ns='http://www.daisy.org/z3986/2005/ncx/',
            opf_ns='http://www.idpf.org/2007/opf',
            xsi_ns='http://www.w3.org/2001/XMLSchema-instance',
            dcterms_ns='http://purl.org/dc/terms/',
            dc_ns='http://purl.org/dc/elements/1.1/',
            cal_ns='http://calibre.kovidgoyal.net/2009/metadata',
            cont_urn='urn:oasis:names:tc:opendocument:xmlns:container',
            mt='application/oebps-package+xml',  # media-type
            style_sheet='stylesheet.css',
            uuid=None,
            nav_point=None,
            nav_uuid=None,
        )

    def __enter__(self):
        return self

    def __exit__(self, typ, value, traceback):
        if value is None:
            if isinstance(self._zip_data, basestring):
                return
            self._write_toc()
            self._write_content()
            self._zip.close()
            self._zip = None
            self.d['nav_point'] = None
            with open(self._output_name, 'wb') as ofp:
                ofp.write(self._zip_data.getvalue())
            # minimal test: listing contents of EPUB
            # os.system('unzip -lv ' + self._output_name)
            return True
        return False

    def add_image_file(self, file_name):
        self._add_image_file(file_name)
        self._count += 1

    def _write_toc(self):
        self._add_from_bytes(self.d['toc_name'], dedent("""\
        <?xml version='1.0' encoding='utf-8'?>
        <ncx xmlns="{ncx_ns}" version="2005-1" xml:lang="eng">
          <head>
            <meta content="{uuid}" name="dtb:uid"/>
            <meta content="2" name="dtb:depth"/>
            <meta content="ruamel.jpeg2epub (0.1)" name="dtb:generator"/>
            <meta content="0" name="dtb:totalPageCount"/>
            <meta content="0" name="dtb:maxPageNumber"/>
          </head>
          <docTitle>
            <text>xx</text>
          </docTitle>
          <navMap>
            <navPoint id="{nav_uuid}" playOrder="1">
              <navLabel>
                <text>Start</text>
              </navLabel>
              <content src="{nav_point}"/>
            </navPoint>
          </navMap>
        </ncx>
        """).format(**self.d))
        self._content.append((self.d['toc_name'], 'ncx',
                              'application/x-dtbncx+xml'))

    def _write_content(self):
        d = self.d.copy()
        manifest = []
        spine = []
        d['manifest'] = ''
        d['spine'] = ''
        for f in self._content:
            manifest.append(
                '<item href="{}" id="{}" media-type="{}"/>'.format(*f))
            if f[1].startswith('html'):
                spine.append('<itemref idref="{}"/>'.format(f[1]))
        d['manifest'] = '\n    '.join(manifest)
        d['spine'] = '\n    '.join(spine)
        d['ts'] = datetime.datetime.utcnow().isoformat() + '+00:00'
        d['series'] = ''
        if self._series:
            d['series'] = \
                u'\n' \
                '<meta name="calibre:series" content="{}"/>' \
                '<meta name="calibre:series_index" content="{}"/>'.format(
                    self._series, self._series_idx)
        self._add_from_bytes(self.d["opf_name"], dedent(u"""\
        <?xml version='1.0' encoding='utf-8'?>
        <package xmlns="{opf_ns}" unique-identifier="uuid_id" version="2.0">
          <metadata xmlns:xsi="{xsi_ns}" xmlns:opf="{opf_ns}"
                    xmlns:dcterms="{dcterms_ns}"
                    xmlns:calibre="{cal_ns}"
                    xmlns:dc="{dc_ns}">
            <dc:language>en</dc:language>
            <dc:creator>{creator}</dc:creator>
            <meta name="calibre:timestamp" content="{ts}"/>
            <meta name="calibre:title_sort" content="{title_sort}"/>
            <meta name="cover" content="cover"/>
            <dc:date>0101-01-01T00:00:00+00:00</dc:date>
            <dc:title>{title}</dc:title>{series}
            <dc:identifier id="uuid_id" opf:scheme="uuid">{uuid}
            </dc:identifier>
            <dc:identifier opf:scheme="calibre">{uuid}</dc:identifier>
          </metadata>
          <manifest>
            {manifest}
          </manifest>
          <spine toc="ncx">
            {spine}
          </spine>
        </package>
        """).format(**d).encode('utf-8'))

    def _add_html(self, title):
        file_name = self._name(False)
        d = self.d.copy()
        d['title'] = title
        d['img_name'] = self._name()
        self._add_from_bytes(file_name, dedent(u"""\
        <?xml version='1.0' encoding='utf-8'?>
        <html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
          <head>
            <title>{title}</title>
            <meta http-equiv="Content-Type" content="text/html; \
        charset=utf-8"/>
          <link href="{style_sheet}" rel="stylesheet" type="text/css"/>
          </head>
          <body class="album">
            <div>
              <img src="{img_name}" class="albumimg" alt="{title}"/>
            </div>
          </body>
        </html>
        """).format(**d).encode('utf-8'))
        self._content.append((file_name, 'html{}'.format(self._count),
                              'application/xhtml+xml'))
        if self.d['nav_point'] is None:
            self.d['nav_point'] = file_name
            self._write_style_sheet()

    def _write_style_sheet(self):
        file_name = self.d['style_sheet']
        self._add_from_bytes(file_name, dedent("""\
        .album {
            display: block;
            font-size: 1em;
            padding: 0;
            margin: 0;
        }
        .albumimg {
            height: auto;
            max-height: 100%;
            max-width: 100%;
            width: auto
        }
        """))
        self._content.append((file_name, 'css', 'text/css'))

    def _name(self, image=True):
        """no leading zero's necessary in zip internal filenames"""
        return '{}.{}'.format(self._count, 'png' if image else 'xhtml')

    def _add_image_file(self, file_name, width=None, height=None,
                        strip=None, max_strip_pixel=None, z=None):
        z = z if z else self.zip  # initializes if not done yet
        self._add_html(file_name)
        # you can compress JPEGs, but with little result (1-8%) and
        # more complex/slow decompression (zip then jpeg)
        # Gain 2.836 Mb -> 2.798 Mb ( ~ 1% difference )
        if width:
            im = EpubImage(file_name)
            z.writestr(self._name(), im.read(), zipfile.ZIP_STORED)
        else:
            z.write(file_name, self._name())
        self._content.append((self._name(), 'img{}'.format(self._count),
                              'image/png'))

    @property
    def zip(self):
        if self._zip is not None:
            return self._zip
        self._zip_data = StringIO()
        # create zip with default compression
        #self._zip_data = '/var/tmp/epubtmp/yy.zip'
        self._zip = zipfile.ZipFile(self._zip_data, "a",
                                    zipfile.ZIP_DEFLATED, True)
        self.d['uuid'] = uuid.uuid4()
        self.d['nav_uuid'] = uuid.uuid4()
        self._add_mimetype()
        self._add_container()
        return self._zip

    def _add_from_bytes(self, file_name, data, no_compression=False):
        self._zip.writestr(
            file_name, data,
            compress_type=zipfile.ZIP_STORED if no_compression else None)

    def _add_mimetype(self):
        self._add_from_bytes('mimetype', dedent("""\
        application/epub+zip
        """).rstrip(), no_compression=True)

    def _add_container(self):
        self._add_from_bytes('META-INF/container.xml', dedent("""\
        <?xml version="1.0"?>
           <container version="1.0" xmlns="{cont_urn}">
          <rootfiles>
            <rootfile full-path="{opf_name}" media-type="{mt}"/>
          </rootfiles>
        </container>
        """).rstrip().format(**self.d))


def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--title", "-t", required=True)
    parser.add_argument("--title-sort", help="alternative title for sorting")
    parser.add_argument(
        "--output", "-o",
        help="epub name if not specified, derived from title",
    )
    parser.add_argument("--series", help="series name")
    parser.add_argument("--index", help="series index")
    parser.add_argument("--creator", help="Creator/Author")
    parser.add_argument("dirs", nargs="+")
    args = parser.parse_args()
    with Jpeg2Epub(args.title, title_sort=args.title_sort,
                   file_name=args.output,
                   series=args.series, series_idx=args.index,
                   creator=args.creator,  verbose=0) as j2e:
        for dir in args.dirs:
            entries = sorted(os.listdir(dir))
            for entry in entries:
                file = dir + entry
                j2e.add_image_file(file)


if __name__ == "__main__":
    main()