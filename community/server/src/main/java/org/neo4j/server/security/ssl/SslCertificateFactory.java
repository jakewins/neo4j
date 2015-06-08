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
package org.neo4j.server.security.ssl;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Date;
import javax.crypto.NoSuchPaddingException;

public class SslCertificateFactory {

    private static final String CERTIFICATE_TYPE = "X.509";
    private static final String KEY_ENCRYPTION = "RSA";
    
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void createSelfSignedCertificate(File certificatePath, File privateKeyPath, String hostName)
            throws NoSuchAlgorithmException, CertificateEncodingException, NoSuchProviderException, InvalidKeyException,
            SignatureException, IOException
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( KEY_ENCRYPTION );
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber( BigInteger.valueOf( new SecureRandom().nextInt() ).abs() );
        certGen.setIssuerDN( new X509Principal( "CN=" + hostName + ", OU=None, O=None L=None, C=None" ) );
        certGen.setNotBefore( new Date( System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30 ) );
        certGen.setNotAfter( new Date( System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10) ) );
        certGen.setSubjectDN( new X509Principal( "CN=" + hostName + ", OU=None, O=None L=None, C=None" ) );

        certGen.setPublicKey( keyPair.getPublic() );
        certGen.setSignatureAlgorithm( "MD5WithRSAEncryption" );

        Certificate certificate = certGen.generate( keyPair.getPrivate(), "BC");

        ensureFolderExists(certificatePath.getParentFile());
        ensureFolderExists(privateKeyPath.getParentFile());

        try(FileOutputStream certStream = new FileOutputStream(certificatePath);
            FileOutputStream keyStream = new FileOutputStream( privateKeyPath ))
        {
            certStream.write( certificate.getEncoded() );
            keyStream.write( keyPair.getPrivate().getEncoded() );
        }
    }

    public Certificate[] loadCertificates(File certFile) throws CertificateException, IOException
    {
        try(FileInputStream in = new FileInputStream(certFile))
        {
            CertificateFactory factory = CertificateFactory.getInstance( CERTIFICATE_TYPE );
            Collection<? extends Certificate> certificates = factory.generateCertificates( in );
            return certificates.toArray(new Certificate[certificates.size()]);
        }
    }

    public PrivateKey loadPrivateKey(File privateKeyFile)
            throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException 
    {
        try(DataInputStream in = new DataInputStream(new FileInputStream(privateKeyFile)))
        {
            byte[] keyBytes = new byte[(int) privateKeyFile.length()];
            in.readFully( keyBytes );

            KeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            return KeyFactory.getInstance(KEY_ENCRYPTION).generatePrivate(keySpec);
        }
    }

    private void ensureFolderExists(File path)
    {
        if(!path.exists())
        {
            path.mkdirs();
        }
    }

}
