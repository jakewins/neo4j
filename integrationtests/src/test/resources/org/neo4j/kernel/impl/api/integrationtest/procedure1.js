for (var product in Iterator(neo4j_db.findNodesByLabelAndProperty(label('PRODUCT'), 'ID', id)))
{
    for (var row in Iterator(procs_getChain(product.getId())))
    {
        for(var promotedRel in Iterator(row.link.getRelationships(neo4j_OUTGOING, [type('APPLY'), type('EXCLUDE')])))
        {
            var promotion = promotedRel.getEndNode();
            yield record( promotion.getProperty("PROMOTION_ID"),
                    promotion.getProperty("PARENT"),
                    promotedRel.getType().name());
        }
    }
}
