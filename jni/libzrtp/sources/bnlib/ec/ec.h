/*
 * Copyright (C) 2012 Werner Dittmann
 * All rights reserved. For licensing and other legal details, see the file legal.c.
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 *
 */
#ifndef _EC_H_
#define _EC_H_

#include <bn.h>

/**
 * @file ec.h
 * @brief Elliptic curve functions for bnlib
 * @defgroup BNLIB_EC Elliptic curve functions
 * @{
 */

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct BigNum BigNum;

typedef enum {
    NIST192P = 1,
    NIST224P = 2,
    NIST256P = 3,
    NIST384P = 4,
    NIST521P = 5
} NistCurves;

/**
 * @brief This structure contains the value of NIST EC curves over Prime Fields.
 *
 * The <b>a</b> curve parameter is the constant -3 and is computed during initialization
 * of the curve structure.
 *
 * The field names correspond to the variable names defined in NIST FIPS 186-3, E.1.2
 */
typedef struct {
    BigNum _p;
    BigNum _n;
    BigNum _SEED;
    BigNum _c;
    BigNum _a;
    BigNum _b;
    BigNum _Gx;
    BigNum _Gy;
    /* Pointers to the BigNum structures, for better readability mainly */
    BigNum *p;
    BigNum *n;
    BigNum *SEED;
    BigNum *c;
    BigNum *a;
    BigNum *b;
    BigNum *Gx;
    BigNum *Gy;
    /* some scratch pad variables, the EC algorithms use them to 
       avoid to much memory allocation/deallocatio0n overhead */
  BigNum _S1, _U1, _H, _R, _t0, _t1, _t2, _t3;
  BigNum *S1, *U1, *H, *R, *t0, *t1, *t2, *t3;
} NistECpCurve;

/**
 * \brief This structure contains the x, y affine coordinates and the z value if we
 *        use projective coordinates during EC point arithmetic.
 */
typedef struct _EcPoint {
    BigNum *x, *y, *z;
    BigNum tx, ty, tz;
} EcPoint;

/**
 * \brief          Marco to initialize a EC point structure.
 *
 * \param P        Address of the EC point structure
 */
#define INIT_EC_POINT(P) {EcPoint *e = P; e->x = &e->tx; e->y = &e->ty; e->z = &e->tz; bnBegin(e->x); bnBegin(e->y); bnBegin(e->z);}

/**
 * \brief          Marco to free a EC point structure.
 *
 * \param P        Address of the EC point structure
 */
#define FREE_EC_POINT(P) {EcPoint *e = P; bnEnd(e->x); bnEnd(e->y); bnEnd(e->z);}

/**
 * \brief          Marco to set a EC point structure to the curve's base point.
 *
 * \param C        Address of the NistECpCurve structure.
 *
 * \param P        Address of the EC point structure.
 */
#define SET_EC_BASE_POINT(C, P) {EcPoint *e = P;  const NistECpCurve *c = C; bnCopy(e->x, c->Gx); bnCopy(e->y, c->Gy); bnSetQ(e->z, 1);}

/**
 * \brief          Get NIST EC curve parameters.
 *
 *                 Before reusing a EC curve structure make sure to call ecFreeCurveNistECp
 *                 to return memory.
 *
 * \param curveId  Which curve to initialize
 *
 * \param curve    Pointer to a NistECpCurve structure
 *
 * \return         0 if successful, or a POLARSSL_ERR_EC_XXX/ POLARSSL_ERR_MPI_XXX error code.
 *
 * \note           Call ecFreeCurveNistECp to return allocated memory.
 */
int ecGetCurveNistECp(NistCurves curveId, NistECpCurve *curve);


/**
 * \brief          Free NIST EC curve parameters.
 *
 * \param curve    Pointer to a NistECpCurve structure
 *
 * \note           Curve parameters must be initialized calling ecGetCurveNistECp.
 */
void ecFreeCurveNistECp(NistECpCurve *curve);

/**
 * \brief          Double an EC point.
 *
 *                 This function uses affine coordinates to perform the computations. For
 *                 further reference see RFC 6090 or the standard work <i>Guide to Elliptic
 *                 Curve Cryptography</i>.
 *
 * \param          curve  Address of Nist EC curve structure
 * \param          R      Address of resulting EC point structure
 * \param          P      Address of the EC point structure
 *
 * \return         0 if successful, or a POLARSSL_ERR_EC_XXX / POLARSSL_ERR_MPI_XXX error code.
 */
int ecDoublePoint(const NistECpCurve *curve, EcPoint *R, const EcPoint *P);

/**
 * \brief          Add two EC points.
 *
 *                 This function uses affine coordinates to perform the computations. For
 *                 further reference see RFC 6090 or the standard work <i>Guide to Elliptic
 *                 Curve Cryptography</i>.
 *
 * \param          curve  Address of Nist EC curve structure
 * \param          R      Address of resulting EC point structure
 * \param          P      Address of the first EC point structure
 * \param          Q      Address of the second EC point structure
 *
 * \return         0 if successful, or a POLARSSL_ERR_EC_XXX / POLARSSL_ERR_MPI_XXX error code.
 */
int ecAddPoint(const NistECpCurve *curve, EcPoint *R, const EcPoint *P, const EcPoint *Q);

/**
 * \brief          Mulitply an EC point with a scalar value.
 *
 * \param          curve  Address of Nist EC curve structure
 * \param          R      Address of resulting EC point structure
 * \param          P      Address of the EC point structure
 * \param          scalar Address of the scalar multi-precision integer value
 *
 * \return         0 if successful, or a POLARSSL_ERR_EC_XXX / POLARSSL_ERR_MPI_XXX error code.
 */
int ecMulPointScalar(const NistECpCurve *curve, EcPoint *R, const EcPoint *P, const BigNum *scalar);

/**
 * \brief          Convert an EC point from Jacobian projective coordinates to normal affine x/y coordinates.
 *
 * \param          curve  Address of Nist EC curve structure
 * \param          R      Address of EC point structure that receives the x/y coordinates
 * \param          P      Address of the EC point structure that contains the jacobian x/y/z coordinates.
 *
 * \return         0 if successful, or a POLARSSL_ERR_EC_XXX / POLARSSL_ERR_MPI_XXX error code.
 */
int ecGetAffine(const NistECpCurve *curve, EcPoint *R, const EcPoint *P);

/**
 * @brief Generate a random number.
 *
 * The method generates a random number and checks if it matches the curve restricitions.
 * Use this number to generate a ECDH public key.
 *
 * @param curve the NIST curve to use.
 *
 * @param d receives the generated random number.
 */
int ecGenerateRandomNumber(const NistECpCurve *curve, BigNum *d);

/*
 * Some additional functions that are not available in bnlib
 */
int bnAddMod_ (struct BigNum *rslt, struct BigNum *n1, struct BigNum *mod);

int bnAddQMod_ (struct BigNum *rslt, unsigned n1, struct BigNum *mod);

int bnSubMod_ (struct BigNum *rslt, struct BigNum *n1, struct BigNum *mod);

int bnSubQMod_ (struct BigNum *rslt, unsigned n1, struct BigNum *mod);

int bnMulMod_ (struct BigNum *rslt, struct BigNum *n1, struct BigNum *n2, struct BigNum *mod);

int bnMulQMod_ (struct BigNum *rslt, struct BigNum *n1, unsigned n2, struct BigNum *mod);

int bnSquareMod_ (struct BigNum *rslt, struct BigNum *n1, struct BigNum *mod);

#ifdef __cplusplus
}
#endif

/**
 * @}
 */

#endif
