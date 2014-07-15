/*___Generated_by_IDEA___*/

/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/alision/dev/git/sflphone-android/src/org/sflphone/service/ISipService.aidl
 */
package org.sflphone.service;
public interface ISipService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.sflphone.service.ISipService
{
private static final java.lang.String DESCRIPTOR = "org.sflphone.service.ISipService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.sflphone.service.ISipService interface,
 * generating a proxy if needed.
 */
public static org.sflphone.service.ISipService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.sflphone.service.ISipService))) {
return ((org.sflphone.service.ISipService)iin);
}
return new org.sflphone.service.ISipService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_getCallDetails:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.Map _result = this.getCallDetails(_arg0);
reply.writeNoException();
reply.writeMap(_result);
return true;
}
case TRANSACTION_placeCall:
{
data.enforceInterface(DESCRIPTOR);
org.sflphone.model.SipCall _arg0;
if ((0!=data.readInt())) {
_arg0 = org.sflphone.model.SipCall.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.placeCall(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_refuse:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.refuse(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_accept:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.accept(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_hangUp:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.hangUp(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_hold:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.hold(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unhold:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.unhold(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getAccountList:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getAccountList();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_addAccount:
{
data.enforceInterface(DESCRIPTOR);
java.util.Map _arg0;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg0 = data.readHashMap(cl);
java.lang.String _result = this.addAccount(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_removeAccount:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.removeAccount(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setAccountOrder:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setAccountOrder(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getAccountDetails:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.Map _result = this.getAccountDetails(_arg0);
reply.writeNoException();
reply.writeMap(_result);
return true;
}
case TRANSACTION_getAccountTemplate:
{
data.enforceInterface(DESCRIPTOR);
java.util.Map _result = this.getAccountTemplate();
reply.writeNoException();
reply.writeMap(_result);
return true;
}
case TRANSACTION_registerAllAccounts:
{
data.enforceInterface(DESCRIPTOR);
this.registerAllAccounts();
reply.writeNoException();
return true;
}
case TRANSACTION_setAccountDetails:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.Map _arg1;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg1 = data.readHashMap(cl);
this.setAccountDetails(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getCredentials:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.List _result = this.getCredentials(_arg0);
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_setCredentials:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.List _arg1;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg1 = data.readArrayList(cl);
this.setCredentials(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_setAudioPlugin:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setAudioPlugin(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getCurrentAudioOutputPlugin:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getCurrentAudioOutputPlugin();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getAudioCodecList:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.List _result = this.getAudioCodecList(_arg0);
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getCurrentAudioCodecName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getCurrentAudioCodecName(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_setActiveCodecList:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _arg0;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg0 = data.readArrayList(cl);
java.lang.String _arg1;
_arg1 = data.readString();
this.setActiveCodecList(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getRingtoneList:
{
data.enforceInterface(DESCRIPTOR);
java.util.Map _result = this.getRingtoneList();
reply.writeNoException();
reply.writeMap(_result);
return true;
}
case TRANSACTION_checkForPrivateKey:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.checkForPrivateKey(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_checkCertificateValidity:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.checkCertificateValidity(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_checkHostnameCertificate:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
boolean _result = this.checkHostnameCertificate(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_toggleSpeakerPhone:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.toggleSpeakerPhone(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setRecordPath:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setRecordPath(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getRecordPath:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getRecordPath();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_toggleRecordingCall:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.toggleRecordingCall(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_startRecordedFilePlayback:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.startRecordedFilePlayback(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_stopRecordedFilePlayback:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.stopRecordedFilePlayback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setMuted:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setMuted(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isCaptureMuted:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isCaptureMuted();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_confirmSAS:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.confirmSAS(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getTlsSupportedMethods:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getTlsSupportedMethods();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_playDtmf:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.playDtmf(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_sendTextMessage:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.sflphone.model.SipMessage _arg1;
if ((0!=data.readInt())) {
_arg1 = org.sflphone.model.SipMessage.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
this.sendTextMessage(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_transfer:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.transfer(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_attendedTransfer:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.attendedTransfer(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_removeConference:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.removeConference(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_joinParticipant:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.joinParticipant(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_addParticipant:
{
data.enforceInterface(DESCRIPTOR);
org.sflphone.model.SipCall _arg0;
if ((0!=data.readInt())) {
_arg0 = org.sflphone.model.SipCall.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
java.lang.String _arg1;
_arg1 = data.readString();
this.addParticipant(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_addMainParticipant:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.addMainParticipant(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_detachParticipant:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.detachParticipant(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_joinConference:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.joinConference(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_hangUpConference:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.hangUpConference(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_holdConference:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.holdConference(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unholdConference:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.unholdConference(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isConferenceParticipant:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.isConferenceParticipant(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getConferenceList:
{
data.enforceInterface(DESCRIPTOR);
java.util.Map _result = this.getConferenceList();
reply.writeNoException();
reply.writeMap(_result);
return true;
}
case TRANSACTION_getParticipantList:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.List _result = this.getParticipantList(_arg0);
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getConferenceId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getConferenceId(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getConferenceDetails:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getConferenceDetails(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getCurrentCall:
{
data.enforceInterface(DESCRIPTOR);
org.sflphone.model.Conference _result = this.getCurrentCall();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getConcurrentCalls:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getConcurrentCalls();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getConference:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.sflphone.model.Conference _result = this.getConference(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.sflphone.service.ISipService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public java.util.Map getCallDetails(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.Map _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_getCallDetails, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readHashMap(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void placeCall(org.sflphone.model.SipCall call) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((call!=null)) {
_data.writeInt(1);
call.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_placeCall, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void refuse(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_refuse, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void accept(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_accept, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void hangUp(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_hangUp, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void hold(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_hold, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void unhold(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_unhold, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.List getAccountList() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAccountList, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String addAccount(java.util.Map accountDetails) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeMap(accountDetails);
mRemote.transact(Stub.TRANSACTION_addAccount, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void removeAccount(java.lang.String accoundId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(accoundId);
mRemote.transact(Stub.TRANSACTION_removeAccount, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setAccountOrder(java.lang.String order) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(order);
mRemote.transact(Stub.TRANSACTION_setAccountOrder, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.Map getAccountDetails(java.lang.String accountID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.Map _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(accountID);
mRemote.transact(Stub.TRANSACTION_getAccountDetails, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readHashMap(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.Map getAccountTemplate() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.Map _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAccountTemplate, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readHashMap(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void registerAllAccounts() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_registerAllAccounts, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setAccountDetails(java.lang.String accountId, java.util.Map accountDetails) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(accountId);
_data.writeMap(accountDetails);
mRemote.transact(Stub.TRANSACTION_setAccountDetails, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.List getCredentials(java.lang.String accountID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(accountID);
mRemote.transact(Stub.TRANSACTION_getCredentials, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setCredentials(java.lang.String accountID, java.util.List creds) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(accountID);
_data.writeList(creds);
mRemote.transact(Stub.TRANSACTION_setCredentials, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setAudioPlugin(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_setAudioPlugin, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String getCurrentAudioOutputPlugin() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentAudioOutputPlugin, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List getAudioCodecList(java.lang.String accountID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(accountID);
mRemote.transact(Stub.TRANSACTION_getAudioCodecList, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getCurrentAudioCodecName(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_getCurrentAudioCodecName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setActiveCodecList(java.util.List codecs, java.lang.String accountID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeList(codecs);
_data.writeString(accountID);
mRemote.transact(Stub.TRANSACTION_setActiveCodecList, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.Map getRingtoneList() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.Map _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRingtoneList, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readHashMap(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean checkForPrivateKey(java.lang.String pemPath) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(pemPath);
mRemote.transact(Stub.TRANSACTION_checkForPrivateKey, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean checkCertificateValidity(java.lang.String pemPath) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(pemPath);
mRemote.transact(Stub.TRANSACTION_checkCertificateValidity, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean checkHostnameCertificate(java.lang.String certificatePath, java.lang.String host, java.lang.String port) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(certificatePath);
_data.writeString(host);
_data.writeString(port);
mRemote.transact(Stub.TRANSACTION_checkHostnameCertificate, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
// FIXME

@Override public void toggleSpeakerPhone(boolean toggle) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((toggle)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_toggleSpeakerPhone, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Recording */
@Override public void setRecordPath(java.lang.String path) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(path);
mRemote.transact(Stub.TRANSACTION_setRecordPath, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String getRecordPath() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRecordPath, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean toggleRecordingCall(java.lang.String id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(id);
mRemote.transact(Stub.TRANSACTION_toggleRecordingCall, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean startRecordedFilePlayback(java.lang.String filepath) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filepath);
mRemote.transact(Stub.TRANSACTION_startRecordedFilePlayback, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void stopRecordedFilePlayback(java.lang.String filepath) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filepath);
mRemote.transact(Stub.TRANSACTION_stopRecordedFilePlayback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Mute */
@Override public void setMuted(boolean mute) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((mute)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setMuted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isCaptureMuted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isCaptureMuted, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Security */
@Override public void confirmSAS(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_confirmSAS, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.List getTlsSupportedMethods() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTlsSupportedMethods, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* DTMF */
@Override public void playDtmf(java.lang.String key) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(key);
mRemote.transact(Stub.TRANSACTION_playDtmf, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* IM */
@Override public void sendTextMessage(java.lang.String callID, org.sflphone.model.SipMessage message) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_sendTextMessage, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void transfer(java.lang.String callID, java.lang.String to) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
_data.writeString(to);
mRemote.transact(Stub.TRANSACTION_transfer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void attendedTransfer(java.lang.String transferID, java.lang.String targetID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(transferID);
_data.writeString(targetID);
mRemote.transact(Stub.TRANSACTION_attendedTransfer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Conference related methods */
@Override public void removeConference(java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_removeConference, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void joinParticipant(java.lang.String sel_callID, java.lang.String drag_callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sel_callID);
_data.writeString(drag_callID);
mRemote.transact(Stub.TRANSACTION_joinParticipant, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void addParticipant(org.sflphone.model.SipCall call, java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((call!=null)) {
_data.writeInt(1);
call.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_addParticipant, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void addMainParticipant(java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_addMainParticipant, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void detachParticipant(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_detachParticipant, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void joinConference(java.lang.String sel_confID, java.lang.String drag_confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sel_confID);
_data.writeString(drag_confID);
mRemote.transact(Stub.TRANSACTION_joinConference, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void hangUpConference(java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_hangUpConference, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void holdConference(java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_holdConference, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void unholdConference(java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_unholdConference, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isConferenceParticipant(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_isConferenceParticipant, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.Map getConferenceList() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.Map _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getConferenceList, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readHashMap(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List getParticipantList(java.lang.String confID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(confID);
mRemote.transact(Stub.TRANSACTION_getParticipantList, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getConferenceId(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_getConferenceId, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getConferenceDetails(java.lang.String callID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callID);
mRemote.transact(Stub.TRANSACTION_getConferenceDetails, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.sflphone.model.Conference getCurrentCall() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.sflphone.model.Conference _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentCall, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.sflphone.model.Conference.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List getConcurrentCalls() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getConcurrentCalls, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.sflphone.model.Conference getConference(java.lang.String id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.sflphone.model.Conference _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(id);
mRemote.transact(Stub.TRANSACTION_getConference, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.sflphone.model.Conference.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getCallDetails = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_placeCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_refuse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_accept = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_hangUp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_hold = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_unhold = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getAccountList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_addAccount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_removeAccount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_setAccountOrder = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getAccountDetails = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getAccountTemplate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_registerAllAccounts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_setAccountDetails = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_getCredentials = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_setCredentials = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_setAudioPlugin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_getCurrentAudioOutputPlugin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_getAudioCodecList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_getCurrentAudioCodecName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_setActiveCodecList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_getRingtoneList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
static final int TRANSACTION_checkForPrivateKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
static final int TRANSACTION_checkCertificateValidity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
static final int TRANSACTION_checkHostnameCertificate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
static final int TRANSACTION_toggleSpeakerPhone = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
static final int TRANSACTION_setRecordPath = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
static final int TRANSACTION_getRecordPath = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
static final int TRANSACTION_toggleRecordingCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
static final int TRANSACTION_startRecordedFilePlayback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
static final int TRANSACTION_stopRecordedFilePlayback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
static final int TRANSACTION_setMuted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
static final int TRANSACTION_isCaptureMuted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
static final int TRANSACTION_confirmSAS = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
static final int TRANSACTION_getTlsSupportedMethods = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
static final int TRANSACTION_playDtmf = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
static final int TRANSACTION_sendTextMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
static final int TRANSACTION_transfer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
static final int TRANSACTION_attendedTransfer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
static final int TRANSACTION_removeConference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
static final int TRANSACTION_joinParticipant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
static final int TRANSACTION_addParticipant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
static final int TRANSACTION_addMainParticipant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
static final int TRANSACTION_detachParticipant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 44);
static final int TRANSACTION_joinConference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 45);
static final int TRANSACTION_hangUpConference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 46);
static final int TRANSACTION_holdConference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 47);
static final int TRANSACTION_unholdConference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 48);
static final int TRANSACTION_isConferenceParticipant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 49);
static final int TRANSACTION_getConferenceList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 50);
static final int TRANSACTION_getParticipantList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 51);
static final int TRANSACTION_getConferenceId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 52);
static final int TRANSACTION_getConferenceDetails = (android.os.IBinder.FIRST_CALL_TRANSACTION + 53);
static final int TRANSACTION_getCurrentCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 54);
static final int TRANSACTION_getConcurrentCalls = (android.os.IBinder.FIRST_CALL_TRANSACTION + 55);
static final int TRANSACTION_getConference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 56);
}
public java.util.Map getCallDetails(java.lang.String callID) throws android.os.RemoteException;
public void placeCall(org.sflphone.model.SipCall call) throws android.os.RemoteException;
public void refuse(java.lang.String callID) throws android.os.RemoteException;
public void accept(java.lang.String callID) throws android.os.RemoteException;
public void hangUp(java.lang.String callID) throws android.os.RemoteException;
public void hold(java.lang.String callID) throws android.os.RemoteException;
public void unhold(java.lang.String callID) throws android.os.RemoteException;
public java.util.List getAccountList() throws android.os.RemoteException;
public java.lang.String addAccount(java.util.Map accountDetails) throws android.os.RemoteException;
public void removeAccount(java.lang.String accoundId) throws android.os.RemoteException;
public void setAccountOrder(java.lang.String order) throws android.os.RemoteException;
public java.util.Map getAccountDetails(java.lang.String accountID) throws android.os.RemoteException;
public java.util.Map getAccountTemplate() throws android.os.RemoteException;
public void registerAllAccounts() throws android.os.RemoteException;
public void setAccountDetails(java.lang.String accountId, java.util.Map accountDetails) throws android.os.RemoteException;
public java.util.List getCredentials(java.lang.String accountID) throws android.os.RemoteException;
public void setCredentials(java.lang.String accountID, java.util.List creds) throws android.os.RemoteException;
public void setAudioPlugin(java.lang.String callID) throws android.os.RemoteException;
public java.lang.String getCurrentAudioOutputPlugin() throws android.os.RemoteException;
public java.util.List getAudioCodecList(java.lang.String accountID) throws android.os.RemoteException;
public java.lang.String getCurrentAudioCodecName(java.lang.String callID) throws android.os.RemoteException;
public void setActiveCodecList(java.util.List codecs, java.lang.String accountID) throws android.os.RemoteException;
public java.util.Map getRingtoneList() throws android.os.RemoteException;
public boolean checkForPrivateKey(java.lang.String pemPath) throws android.os.RemoteException;
public boolean checkCertificateValidity(java.lang.String pemPath) throws android.os.RemoteException;
public boolean checkHostnameCertificate(java.lang.String certificatePath, java.lang.String host, java.lang.String port) throws android.os.RemoteException;
// FIXME

public void toggleSpeakerPhone(boolean toggle) throws android.os.RemoteException;
/* Recording */
public void setRecordPath(java.lang.String path) throws android.os.RemoteException;
public java.lang.String getRecordPath() throws android.os.RemoteException;
public boolean toggleRecordingCall(java.lang.String id) throws android.os.RemoteException;
public boolean startRecordedFilePlayback(java.lang.String filepath) throws android.os.RemoteException;
public void stopRecordedFilePlayback(java.lang.String filepath) throws android.os.RemoteException;
/* Mute */
public void setMuted(boolean mute) throws android.os.RemoteException;
public boolean isCaptureMuted() throws android.os.RemoteException;
/* Security */
public void confirmSAS(java.lang.String callID) throws android.os.RemoteException;
public java.util.List getTlsSupportedMethods() throws android.os.RemoteException;
/* DTMF */
public void playDtmf(java.lang.String key) throws android.os.RemoteException;
/* IM */
public void sendTextMessage(java.lang.String callID, org.sflphone.model.SipMessage message) throws android.os.RemoteException;
public void transfer(java.lang.String callID, java.lang.String to) throws android.os.RemoteException;
public void attendedTransfer(java.lang.String transferID, java.lang.String targetID) throws android.os.RemoteException;
/* Conference related methods */
public void removeConference(java.lang.String confID) throws android.os.RemoteException;
public void joinParticipant(java.lang.String sel_callID, java.lang.String drag_callID) throws android.os.RemoteException;
public void addParticipant(org.sflphone.model.SipCall call, java.lang.String confID) throws android.os.RemoteException;
public void addMainParticipant(java.lang.String confID) throws android.os.RemoteException;
public void detachParticipant(java.lang.String callID) throws android.os.RemoteException;
public void joinConference(java.lang.String sel_confID, java.lang.String drag_confID) throws android.os.RemoteException;
public void hangUpConference(java.lang.String confID) throws android.os.RemoteException;
public void holdConference(java.lang.String confID) throws android.os.RemoteException;
public void unholdConference(java.lang.String confID) throws android.os.RemoteException;
public boolean isConferenceParticipant(java.lang.String callID) throws android.os.RemoteException;
public java.util.Map getConferenceList() throws android.os.RemoteException;
public java.util.List getParticipantList(java.lang.String confID) throws android.os.RemoteException;
public java.lang.String getConferenceId(java.lang.String callID) throws android.os.RemoteException;
public java.lang.String getConferenceDetails(java.lang.String callID) throws android.os.RemoteException;
public org.sflphone.model.Conference getCurrentCall() throws android.os.RemoteException;
public java.util.List getConcurrentCalls() throws android.os.RemoteException;
public org.sflphone.model.Conference getConference(java.lang.String id) throws android.os.RemoteException;
}
