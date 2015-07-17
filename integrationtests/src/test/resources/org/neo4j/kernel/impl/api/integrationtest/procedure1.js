for (var product in neo4j.findNodes('PRODUCT', 'ID', id))
{
    for (var row in procs_getChain(product.getId()))
    {
        for(var promotedRel in Iterator(row.link.getRelationships(neo4j.OUTGOING, [type('APPLY'), type('EXCLUDE')])))
        {
            var promotion = promotedRel.getEndNode();
            yield record( promotion.getProperty("PROMOTION_ID"),
                    promotion.getProperty("PARENT"),
                    promotedRel.getType().name());
        }
    }
}
