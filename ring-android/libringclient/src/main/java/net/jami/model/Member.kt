package net.jami.model

class Member {
    enum class Role {
        Left, Invited, Member, Admin
    }

    private var mContact: Contact
    private lateinit var mRole: Role

    val role: Role
        get() = mRole

    constructor(contact: Contact, role: String) {
        mContact = contact
        if (role == "left") {
            mRole = Role.Left
        } else if (role == "invited") {
            mRole = Role.Invited
        } else if (role == "member") {
            mRole = Role.Member
        } else if (role == "admin") {
            mRole = Role.Admin
        }
    }
}