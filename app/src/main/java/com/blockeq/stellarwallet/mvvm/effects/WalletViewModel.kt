package com.blockeq.stellarwallet.mvvm.effects

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.blockeq.stellarwallet.WalletApplication
import com.blockeq.stellarwallet.helpers.Constants.Companion.DEFAULT_ACCOUNT_BALANCE
import com.blockeq.stellarwallet.models.AvailableBalance
import com.blockeq.stellarwallet.models.BalanceState
import com.blockeq.stellarwallet.models.TotalBalance
import com.blockeq.stellarwallet.mvvm.account.AccountRepository
import com.blockeq.stellarwallet.utils.AccountUtils
import com.blockeq.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import org.jetbrains.anko.doAsync
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.effects.EffectResponse
import timber.log.Timber

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val effectsRepository : EffectsRepository = EffectsRepository.getInstance()
    private var walletViewState: MutableLiveData<WalletViewState> = MutableLiveData()
    private var accountResponse: AccountResponse? = null
    private var effectsListResponse: ArrayList<EffectResponse>? = null
    private var state: BalanceState = BalanceState.UPDATING

    init {
        loadAccount(false)
    }

    private fun loadAccount(notify: Boolean){
        AccountRepository.loadAccount().observeForever {
            if (it != null) {
                when (it.httpCode) {
                    200 -> {
                        accountResponse = it.accountResponse
                        if (it.accountResponse != null && effectsListResponse != null) {
                            state = BalanceState.ACTIVE
                            Timber.d("setting state to ACTIVE")
                        }
                    }
                    404 -> {
                        accountResponse = null
                        Timber.d("setting state to NOT_FUNDED")
                        state = BalanceState.NOT_FUNDED
                    }
                    else -> {
                        state = BalanceState.ERROR
                    }
                }

                if (notify) {
                    notifyViewState()
                }
            }
        }
    }

    fun forceRefresh() {
        state = BalanceState.UPDATING
        doAsync {
            loadAccount(true)
            effectsRepository.loadList().observeForever { it ->
                effectsListResponse = it
                if (it != null && accountResponse != null) {
                    state = BalanceState.ACTIVE
                    Timber.d("setting state to ACTIVE")
                }
                notifyViewState()
            }
        }
    }

    fun walletViewState(): MutableLiveData<WalletViewState> {
        notifyViewState()
        forceRefresh()
        return walletViewState
    }

    private fun notifyViewState() {
        val accountId = WalletApplication.wallet.getStellarAccountId()!!
        when(state) {
            BalanceState.ACTIVE -> {
                val availableBalance = getAvailableBalance()
                val totalAvailableBalance = getTotalAssetBalance()
                //TODO fix the mutable null issue here
                walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ACTIVE, accountId, getActiveAssetCode(), availableBalance, totalAvailableBalance, effectsListResponse))
            }
            BalanceState.ERROR -> {
                walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ERROR, accountId, getActiveAssetCode(), null, null, null))
            }
            BalanceState.NOT_FUNDED -> {
                val availableBalance = AvailableBalance("XLM", DEFAULT_ACCOUNT_BALANCE)
                val totalAvailableBalance = TotalBalance(state, "Lumens", "XLM", DEFAULT_ACCOUNT_BALANCE)
                walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.UNFUNDED, accountId, getActiveAssetCode(), availableBalance, totalAvailableBalance, null))
            } else -> {
                // nothing
            }
        }
    }

    private fun getActiveAssetCode() : String {
        return WalletApplication.userSession.currAssetCode
    }

    private fun getActiveAssetName() : String {
        return WalletApplication.userSession.currAssetName
    }

    private fun getAvailableBalance() : AvailableBalance {
        val balance = truncateDecimalPlaces(WalletApplication.wallet.getAvailableBalance())
        return AvailableBalance(getActiveAssetCode(), balance)
    }

    private fun getTotalAssetBalance(): TotalBalance {
        val currAsset = WalletApplication.userSession.currAssetCode
        val assetBalance = truncateDecimalPlaces(AccountUtils.getTotalBalance(currAsset))
        return TotalBalance(BalanceState.ACTIVE, getActiveAssetName(), getActiveAssetCode(), assetBalance)
    }
}
