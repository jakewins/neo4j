package org.neo4j.kernel.impl.transaction;

import org.junit.Test;
import org.neo4j.kernel.api.KernelStatement;

import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.mock;

public class ReferenceCountingKernelStatementTest
{

    @Test
    public void doesNotDelegateCloseUntilAllReferencesHaveClosed() throws Exception
    {
        // Given
        KernelStatement inner = mock(KernelStatement.class);
        ReferenceCountingKernelStatement stmt = new ReferenceCountingKernelStatement( inner );

        // When
        stmt.claimReference();
        stmt.claimReference();
        stmt.close();
        stmt.close();

        // Then
        verify(inner, times(1)).close();
    }

}
