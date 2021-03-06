package com.blockeq.stellarwallet.interfaces

import org.stellar.sdk.Asset

interface ChangeTrustlineListener {
    fun changeTrustline(asset: Asset, isRemoveAsset: Boolean)
}