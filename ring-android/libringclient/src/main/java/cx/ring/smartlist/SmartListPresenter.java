package cx.ring.smartlist;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.HistoryService;
import cx.ring.utils.BlockchainInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

/**
 * Created by hdsousa on 17-03-15.
 */

public class SmartListPresenter extends RootPresenter<SmartListView> implements Observer<ServiceEvent> {

    private AccountService mAccountService;

    private ContactService mContactService;

    private HistoryService mHistoryService;

    private ConversationFacade mConversationFacade;

    private BlockchainInputHandler mBlockchainInputHandler;
    private String mLastBlockchainQuery = null;

    @Inject
    public SmartListPresenter(AccountService mAccountService, ContactService mContactService,
                              HistoryService mHistoryService, ConversationFacade mConversationFacade) {
        this.mAccountService = mAccountService;
        this.mContactService = mContactService;
        this.mHistoryService = mHistoryService;
        this.mConversationFacade = mConversationFacade;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mConversationFacade.removeObserver(this);
    }

    public void init() {
        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
    }

    public void refresh() {
        mConversationFacade.refreshConversations();
        searchForRingIdInBlockchain();
    }

    public void checkIntent() {

    }

    public void queryTextChanged(String query) {
        if (query.equals("")) {
            getView().hideSearchRow();
        } else {
            Account currentAccount = mAccountService.getCurrentAccount();
            if (currentAccount == null) {
                return;
            }

            if (currentAccount.isSip()) {
                // sip search
                getView().displayNewContactRowWithName(query, null);
            } else {

                Uri uri = new Uri(query);
                if (uri.isRingId()) {
                    getView().displayNewContactRowWithName(query, null);
                } else {
                    getView().hideSearchRow();
                }

                // Ring search
                if (mBlockchainInputHandler == null) {
                    mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
                }

                // searching for a ringId or a blockchained username
                if (!mBlockchainInputHandler.isAlive()) {
                    mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
                }

                mBlockchainInputHandler.enqueueNextLookup(query);
            }
        }
    }

    public void newContactClicked(CallContact callContact) {
        if (callContact == null) {
            return;
        }
        startConversation(callContact);
    }

    public void conversationClicked(CallContact callContact) {
        if (callContact == null) {
            return;
        }
        startConversation(callContact);
    }

    public void startConversation(CallContact c) {
        // We add the contact to the current State so that we can
        // get it from whatever part of the app as "an already used contact"
        mContactService.addContact(c);
        getView().goToConversation(c);
    }

    public void deleteConversation(Conversation conversation) {
        mHistoryService.clearHistoryForConversation(conversation);
    }

    private void searchForRingIdInBlockchain() {
        List<Conversation> conversations = mConversationFacade.getConversationsList();
        for (Conversation conversation : conversations) {
            CallContact contact = conversation.getContact();
            if (contact == null) {
                continue;
            }

            Uri contactUri = new Uri(contact.getIds().get(0));
            if (contactUri.isRingId()) {
                return;
            }

            if (contact.getPhones().isEmpty()) {
                mAccountService.lookupName("", "", contact.getDisplayName());
            } else {
                Phone phone = contact.getPhones().get(0);
                if (!phone.getNumber().isRingId()) {
                    mAccountService.lookupName("", "", contact.getDisplayName());
                }
            }
        }
    }

    private void parseEventState(String name, String address, int state) {
        switch (state) {
            case 0:
                // on found
                if (mLastBlockchainQuery.equals(name)) {
                    getView().displayNewContactRowWithName(name, address);
                    mLastBlockchainQuery = null;
                } else {
                    getView().hideSearchRow();
                }

                if (name.equals("") || address.equals("")) {
                    return;
                }

                mConversationFacade.updateConversationContactWithRingId(name, address);
                getView().updateView(mConversationFacade.getConversationsList(), null);
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()) {
                    getView().displayNewContactRowWithName(name, null);
                } else {
                    getView().hideSearchRow();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()) {
                    getView().displayNewContactRowWithName(name, address);
                } else {
                    getView().hideSearchRow();
                }
                break;
        }
    }



    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTERED_NAME_FOUND:
                String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                if (mLastBlockchainQuery.equals("") || !mLastBlockchainQuery.equals(name)) {
                    return;
                }
                String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                parseEventState(name, address, state);
                break;
            case INCOMING_MESSAGE:
            case HISTORY_LOADED:
                getView().updateView(mConversationFacade.getConversationsList(), null);
                break;
            case CONVERSATIONS_CHANGED:
                refresh();
                break;
        }
    }
}
