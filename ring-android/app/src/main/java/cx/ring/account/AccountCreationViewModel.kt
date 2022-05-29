package cx.ring.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AccountCreationViewModel: ViewModel() {
    val model = MutableLiveData(AccountCreationModelImpl())
}