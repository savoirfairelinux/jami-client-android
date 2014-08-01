



extern struct callmanager_callback wrapper_callback_struct;
void on_new_call_created_wrapper (const std::string& accountID,
                                         const std::string& callID,
                                         const std::string& to);
void on_call_state_changed_wrapper(const std::string& callID,
                                          const std::string& to);
void on_incoming_call_wrapper (const std::string& accountID,
                                      const std::string& callID,
                                      const std::string& from);
void on_transfer_state_changed_wrapper (const std::string& result);

void on_conference_created_wrapper (const std::string& confID);
void on_conference_removed_wrapper (const std::string& confID);
void on_conference_state_changed_wrapper(const std::string& confID,const std::string& state);
void on_incoming_message_wrapper(const std::string& ID, const std::string& from, const std::string& msg);
void on_newPresSubClientNotification_wrapper(const std::string& uri, const std::string& basic, const std::string& note);
void on_newPresSubServerRequest_wrapper(const std::string& remote);

void on_secure_sdes_on_wrapper(const std::string& callID);
void on_secure_sdes_off_wrapper(const std::string& callID);
void on_secure_zrtp_on_wrapper(const std::string& callID,const std::string& cipher);
void on_secure_zrtp_off_wrapper(const std::string& callID);
void on_show_sas_wrapper(const std::string& callID, const std::string& sas, const bool& verified);
void on_zrtp_not_supported_wrapper(const std::string& callID);
void on_zrtp_negociation_failed_wrapper(const std::string& callID, const std::string& reason, const std::string& severity);
void on_rtcp_report_received_wrapper(const std::string& callID, const std::map<std::basic_string<char>, int>& stats);

extern struct configurationmanager_callback wrapper_configurationcallback_struct;
extern void on_accounts_changed_wrapper ();
extern void on_account_state_changed_wrapper (const std::string& accoundID, int const& state);
extern void on_account_state_changed_with_code_wrapper (const std::string& accoundID, const std::string& state, const int32_t& code);
extern std::vector<int> get_hardware_audio_format_wrapper();

void on_record_playback_filepath_wrapper(const std::string& id, const std::string& filename);
void on_recording_state_changed_wrapper(const std::string& callID, const bool& state);
