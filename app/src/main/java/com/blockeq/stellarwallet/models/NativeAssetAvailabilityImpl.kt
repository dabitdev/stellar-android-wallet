package com.blockeq.stellarwallet.models

data class NativeAssetAvailabilityImpl(override val baseAmount: Float,
                                       override val additionalSignersCount: Int,
                                       override val additionalSignersAmount: Float,
                                       override val trustLinesCount: Int,
                                       override val trustLinesAmount: Float,
                                       override val openOffersCount: Int,
                                       override val openOffersAmount: Float,
                                       override val postedForTradeAmount: Float,
                                       override val total: Float) : NativeAssetAvailability {
    override val totalAvailable: Float
        get() = total - baseAmount - additionalSignersAmount - trustLinesAmount - openOffersAmount - postedForTradeAmount
}
