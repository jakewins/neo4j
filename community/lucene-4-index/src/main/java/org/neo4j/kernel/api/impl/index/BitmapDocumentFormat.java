/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.neo4j.kernel.api.impl.index.bitmaps.Bitmap;
import org.neo4j.kernel.api.impl.index.bitmaps.BitmapFormat;

import static java.lang.String.format;

public enum BitmapDocumentFormat
{
    _32( BitmapFormat._32 )
    {
        @Override
        public IndexableField labelField( long key, long bitmap )
        {
            assert (bitmap & 0xFFFFFFFF00000000L) == 0 :
                "Tried to store a bitmap as int, but which had values outside int limits";

            return new IntField( label( key ), (int)bitmap, INT_STORED_NOT_INDEXED );
        }
    },
    _64( BitmapFormat._64 )
    {
        @Override
        public IndexableField labelField( long key, long bitmap )
        {
            return new LongField( label( key ), bitmap, LONG_STORED_NOT_INDEXED );
        }
    };

    public static final FieldType INT_STORED_NOT_INDEXED = new FieldType( IntField.TYPE_STORED );
    public static final FieldType LONG_STORED_NOT_INDEXED = new FieldType( LongField.TYPE_STORED );

    static
    {
        INT_STORED_NOT_INDEXED.setIndexed( false );
        INT_STORED_NOT_INDEXED.freeze();

        LONG_STORED_NOT_INDEXED.setIndexed( false );
        LONG_STORED_NOT_INDEXED.freeze();
    }

    public static final String RANGE = "range", LABEL = "label";
    private final BitmapFormat format;

    private BitmapDocumentFormat( BitmapFormat format )
    {
        this.format = format;
    }

    @Override
    public String toString()
    {
        return format( "%s{%s bit}", getClass().getSimpleName(), format );
    }

    public BitmapFormat bitmapFormat()
    {
        return format;
    }

    public long rangeOf( Document doc )
    {
        return Long.parseLong( doc.get( RANGE ) );
    }

    public long rangeOf( IndexableField field )
    {
        return Long.parseLong( field.stringValue() );
    }

    public long mapOf( Document doc, long labelId )
    {
        return bitmap( doc.getField( label( labelId ) ) );
    }

    public Query labelQuery( long labelId )
    {
        return new TermQuery( new Term( LABEL, Long.toString( labelId ) ) );
    }

    public Query rangeQuery( long range )
    {
        return new TermQuery( new Term( RANGE, Long.toString( range) ) );
    }

    public IndexableField rangeField( long range )
    {
        // TODO: figure out what flags to set on the field
        return new StringField( RANGE, Long.toString( range ), Field.Store.YES );
    }

    public abstract IndexableField labelField( long key, long bitmap );

    public IndexableField labelSearchField( long label )
    {
        // Label Search Fields are INDEX ONLY (not stored in the document)
        return new StringField( LABEL, Long.toString( label ), Field.Store.NO );
    }

    public void addLabelField( Document document, long label, Bitmap bitmap )
    {
        document.add( labelField( label, bitmap ) );
        document.add( labelSearchField( label ) );
    }

    public IndexableField labelField( long key, Bitmap value )
    {
        return labelField( key, value.bitmap() );
    }

    String label( long key )
    {
        return Long.toString( key );
    }

    public long labelId( IndexableField field )
    {
        return Long.parseLong( field.name() );
    }

    public Term rangeTerm( long range )
    {
        return new Term( RANGE, Long.toString( range ) );
    }

    public Term rangeTerm( Document document )
    {
        return new Term( RANGE, document.get( RANGE ) );
    }

    public boolean isRangeField( IndexableField field )
    {
        String fieldName = field.name();
        return RANGE.equals( fieldName ) || LABEL.equals( fieldName );
    }

    public Bitmap readBitmap( IndexableField field )
    {
        return new Bitmap( bitmap( field ) );
    }

    private long bitmap( IndexableField field )
    {
        if ( field == null )
        {
            return 0;
        }
        return field.numericValue().longValue();
    }
}
