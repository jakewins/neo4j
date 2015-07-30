/*
 * Copyright (c) 2002-2015 "Neo Technology,"
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
const Product = label('Product'),
      APPLY   = relType('APPLY'),
      EXCLUDE = relType('EXCLUDE');

for (let product of neo4j.db.findNodes(Product, 'ID', id))
{
    print(procs.getChain);
    for (let row of procs.getChain(product.id))
    {
        for(let promotedRel of row.link.getRelationships(neo4j.OUTGOING, [APPLY, EXCLUDE]))
        {
            let promo = promotedRel.endNode;
            yield record( promo.getProperty("PROMOTION_ID"), promo.getProperty("PARENT"), promotedRel.type );
        }
    }
}
