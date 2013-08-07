/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api;

import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.pool.Pool;

public interface ReuseableKernelTransaction extends KernelTransaction, Pool.Item
{
}
