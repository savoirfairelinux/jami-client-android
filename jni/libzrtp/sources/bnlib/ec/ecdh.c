/*
 * Copyright (C) 2012 Werner Dittmann
 * All rights reserved. For licensing and other legal details, see the file legal.c.
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 *
 */

#include <ec/ec.h>
#include <ec/ecdh.h>

int ecdhGeneratePublic(const EcCurve *curve, EcPoint *Q, const BigNum *d)
{
    EcPoint G;

    INIT_EC_POINT(&G);
    SET_EC_BASE_POINT(curve, &G);

    ecMulPointScalar(curve, Q, &G, d);
    ecGetAffine(curve, Q, Q);

    FREE_EC_POINT(&G);

    return ecCheckPubKey(curve, Q);
}

int ecdhComputeAgreement(const EcCurve *curve, BigNum *agreement, const EcPoint *Q, const BigNum *d)
{
    EcPoint t0;

    INIT_EC_POINT(&t0);

    ecMulPointScalar(curve, &t0, Q, d);
    ecGetAffine(curve, &t0, &t0);
    /* TODO: check for infinity here */

    bnCopy(agreement, t0.x);

    FREE_EC_POINT(&t0);

    return 0;
}
