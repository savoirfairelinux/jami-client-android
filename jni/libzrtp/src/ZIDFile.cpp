/*
  Copyright (C) 2006-2008 Werner Dittmann

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * Authors: Werner Dittmann <Werner.Dittmann@t-online.de>
 */
// #define UNIT_TEST

#include <string>
#include <time.h>
#include <stdlib.h>
#include <unistd.h>

#include <libzrtpcpp/ZIDFile.h>


static ZIDFile* instance;
static int errors = 0;  // maybe we will use as member of ZIDFile later...

void ZIDFile::createZIDFile(char* name) {
    zidFile = fopen(name, "wb+");
    // New file, generate an associated random ZID and save
    // it as first record
    if (zidFile != NULL) {
        unsigned int* ip;
        ip = (unsigned int*) associatedZid;
        srand(time(NULL));
        *ip++ = rand();
        *ip++ = rand();
        *ip = rand();

        ZIDRecord rec(associatedZid);
        rec.setOwnZIDRecord();
        fseek(zidFile, 0L, SEEK_SET);
        if (fwrite(rec.getRecordData(), rec.getRecordLength(), 1, zidFile) < 1)
            ++errors;
        fflush(zidFile);
    }
}

/**
 * Migrate old ZID file format to new one.
 *
 * If ZID file is old format:
 * - close it, rename it, then re-open
 * - create ZID file for new format
 * - copy over contents and flags.
 */
void ZIDFile::checkDoMigration(char* name) {
    FILE* fdOld;
    unsigned char inb[2];
    zidrecord1_t recOld;

    fseek(zidFile, 0L, SEEK_SET);
    if (fread(inb, 2, 1, zidFile) < 1) {
        ++errors;
        inb[0] = 0;
    }

    if (inb[0] > 0) {           // if it's new format just return
        return;
    }
    fclose(zidFile);            // close old ZID file
    zidFile = NULL;

    // create save file name, rename and re-open
    // if rename fails, just unlink old ZID file and create a brand new file
    // just a little inconvenience for the user, need to verify new SAS
    std::string fn = std::string(name) + std::string(".save");
    if (rename(name, fn.c_str()) < 0) {
	unlink(name);
        createZIDFile(name);
        return;
    }
    fdOld = fopen(fn.c_str(), "rb");    // reopen old format in read only mode

    // Get first record from old file - is the own ZID
    fseek(fdOld, 0L, SEEK_SET);
    if (fread(&recOld, sizeof(zidrecord1_t), 1, fdOld) != 1) {
        fclose(fdOld);
        return;
    }
    if (recOld.ownZid != 1) {
        fclose(fdOld);
        return;
    }
    zidFile = fopen(name, "wb+");    // create new format file in binary r/w mode
    if (zidFile == NULL) {
        return;
    }
    // create ZIDRecord in new format, copy over own ZID and write the record
    ZIDRecord rec(recOld.identifier);
    rec.setOwnZIDRecord();
    if (fwrite(rec.getRecordData(), rec.getRecordLength(), 1, zidFile) < 1)
        ++errors;

    // now copy over all valid records from old ZID file format.
    // Sequentially read old records, sequentially write new records
    int numRead;
    do {
        numRead = fread(&recOld, sizeof(zidrecord1_t), 1, fdOld);
        if (numRead == 0) {     // all old records processed
            break;
        }
        // skip own ZID record and invalid records
        if (recOld.ownZid == 1 || recOld.recValid == 0) {
            continue;
        }
        ZIDRecord rec2(recOld.identifier);
        rec2.setValid();
        if (recOld.rs1Valid & SASVerified) {
            rec2.setSasVerified();
        }
        rec2.setNewRs1(recOld.rs2Data);
        rec2.setNewRs1(recOld.rs1Data);
        if (fwrite(rec2.getRecordData(), rec2.getRecordLength(), 1, zidFile) < 1)
            ++errors;

    } while (numRead == 1);
    fflush(zidFile);
}

ZIDFile::~ZIDFile() {
    close();
}

ZIDFile* ZIDFile::getInstance() {

    if (instance == NULL) {
        instance = new ZIDFile();
    }
    return instance;
}

int ZIDFile::open(char* name) {

    // check for an already active ZID file
    if (zidFile != NULL) {
        return 0;
    }
    if ((zidFile = fopen(name, "rb+")) == NULL) {
        createZIDFile(name);
    } else {
        checkDoMigration(name);
        if (zidFile != NULL) {
            ZIDRecord rec;
            fseek(zidFile, 0L, SEEK_SET);
            if (fread(rec.getRecordData(), rec.getRecordLength(), 1, zidFile) != 1) {
                fclose(zidFile);
                zidFile = NULL;
                return -1;
            }
            if (!rec.isOwnZIDRecord()) {
                fclose(zidFile);
                zidFile = NULL;
                return -1;
            }
            memcpy(associatedZid, rec.getIdentifier(), IDENTIFIER_LEN);
        }
    }
    return ((zidFile == NULL) ? -1 : 1);
}

void ZIDFile::close() {

    if (zidFile != NULL) {
        fclose(zidFile);
        zidFile = NULL;
    }
}

unsigned int ZIDFile::getRecord(ZIDRecord* zidRecord) {
    unsigned long pos;
    ZIDRecord rec;
    int numRead;

    // set read pointer behind first record (
    fseek(zidFile, rec.getRecordLength(), SEEK_SET);

    do {
        pos = ftell(zidFile);
        numRead = fread(rec.getRecordData(), rec.getRecordLength(), 1, zidFile);
        if (numRead == 0) {
            break;
        }

        // skip own ZID record and invalid records
        if (rec.isOwnZIDRecord() || !rec.isValid()) {
            continue;
        }

    } while (numRead == 1 &&
             memcmp(zidRecord->getIdentifier(), rec.getIdentifier(), IDENTIFIER_LEN) != 0);

    // If we reached end of file, then no record with the ZID
    // found. We need to create a new ZID record.
    if (numRead == 0) {
        // create new record
        ZIDRecord rec1(zidRecord->getIdentifier());
        rec1.setValid();
        if (fwrite(rec1.getRecordData(), rec1.getRecordLength(), 1, zidFile) < 1)
            ++errors;
        memcpy(zidRecord->getRecordData(), rec1.getRecordData(), rec1.getRecordLength());
    } else {
        // Copy the read data into caller's the record storage
        memcpy(zidRecord->getRecordData(), rec.getRecordData(), rec.getRecordLength());
    }

    //  remember position of record in file for save operation
    zidRecord->setPosition(pos);
    return 1;
}

unsigned int ZIDFile::saveRecord(ZIDRecord *zidRecord) {

    fseek(zidFile, zidRecord->getPosition(), SEEK_SET);
    if (fwrite(zidRecord->getRecordData(), zidRecord->getRecordLength(), 1, zidFile) < 1)
        ++errors;
    fflush(zidFile);
    return 1;
}


#ifdef UNIT_TEST

#include <iostream>
#include <unistd.h>
using namespace std;

static void hexdump(const char* title, const unsigned char *s, int l) {
    int n=0;

    if (s == NULL) return;

    fprintf(stderr, "%s",title);
    for (; n < l ; ++n) {
        if ((n%16) == 0)
            fprintf(stderr, "\n%04x",n);
        fprintf(stderr, " %02x",s[n]);
    }
    fprintf(stderr, "\n");
}

int main(int argc, char *argv[]) {

    unsigned char myId[IDENTIFIER_LEN];
    ZIDFile *zid = ZIDFile::getInstance();

    unlink("testzid2");
    zid->open("testzid2");
    hexdump("My ZID: ", zid->getZid(), IDENTIFIER_LEN);
    memcpy(myId, zid->getZid(), IDENTIFIER_LEN);
    zid->close();

    zid->open("testzid2");
    if (memcmp(myId, zid->getZid(), IDENTIFIER_LEN) != 0) {
        cerr << "Wrong ZID in testfile" << endl;
        return 1;
    }

    // Create a new ZID record for peer ZID "123456789012"
    ZIDRecord zr3((unsigned char*) "123456789012");
    zid->getRecord(&zr3);
    if (!zr3.isValid()) {
        cerr << "New ZID record '123456789012' not set to valid" << endl;
        return 1;
    }
    zid->saveRecord(&zr3);

    // create a second record with peer ZID "210987654321"
    ZIDRecord zr4((unsigned char*) "210987654321");
    zid->getRecord(&zr4);
    if (!zr4.isValid()) {
        cerr << "New ZID record '210987654321' not set to valid" << endl;
        return 1;
    }
    zid->saveRecord(&zr4);

    // now set a first RS1 with default expiration interval, check
    // if set correctly, valid flag and expiration interval
    zr3.setNewRs1((unsigned char*) "11122233344455566677788899900012");
    if (memcmp(zr3.getRs1(), "11122233344455566677788899900012", RS_LENGTH) != 0) {
        cerr << "RS1 was not set (111...012)" << endl;
        return 1;
    }
    if (!zr3.isRs1Valid()) {
        cerr << "RS1 was not set to valid state (111...012)" << endl;
        return 1;
    }
    if (!zr3.isRs1NotExpired()) {
        cerr << "RS1 expired (111...012)" << endl;
        return 1;
    }
    if (zr3.isRs2Valid()) {
        cerr << "RS2 was set to valid state (111...012)" << endl;
        return 1;
    }
    zid->saveRecord(&zr3);

    // create a second RS1, RS2 will become the first RS1,  check
    // if set correctly, valid flag and expiration interval for both
    // RS1 and RS2
    zr3.setNewRs1((unsigned char*) "00099988877766655544433322211121");
    if (memcmp(zr3.getRs1(), "00099988877766655544433322211121", RS_LENGTH) != 0) {
        cerr << "RS1 was not set (000...121)" << endl;
        return 1;
    }
    if (!zr3.isRs1Valid()) {
        cerr << "RS1 was not set to valid state (000...121)" << endl;
        return 1;
    }
    if (!zr3.isRs1NotExpired()) {
        cerr << "RS1 expired (000...121)" << endl;
        return 1;
    }
    if (memcmp(zr3.getRs2(), "11122233344455566677788899900012", RS_LENGTH) != 0) {
        cerr << "RS2 was not set (111...012)" << endl;
        return 1;
    }
    if (!zr3.isRs2Valid()) {
        cerr << "RS2 was not set to valid state (111...012)" << endl;
        return 1;
    }
    if (!zr3.isRs2NotExpired()) {
        cerr << "RS2 expired (111...012)" << endl;
        return 1;
    }
    zid->saveRecord(&zr3);

    zid->close();

    // Reopen, check if first record is still valid, RSx vaild and
    // not expired. Then manipulate 2nd record.
    zid->open("testzid2");

    ZIDRecord zr3a((unsigned char*) "123456789012");
    zid->getRecord(&zr3a);
    if (!zr3a.isValid()) {
        cerr << "Re-read ZID record '123456789012' not set to valid" << endl;
        return 1;
    }
    if (memcmp(zr3a.getRs1(), "00099988877766655544433322211121", RS_LENGTH) != 0) {
        cerr << "re-read RS1 was not set (000...121)" << endl;
        return 1;
    }
    if (!zr3a.isRs1Valid()) {
        cerr << "Re-read RS1 was not set to valid state (000...121)" << endl;
        return 1;
    }
    if (!zr3a.isRs1NotExpired()) {
        cerr << "re-read RS1 expired (000...121)" << endl;
        return 1;
    }
    if (memcmp(zr3a.getRs2(), "11122233344455566677788899900012", RS_LENGTH) != 0) {
        cerr << "re-read RS2 was not set (111...012)" << endl;
        return 1;
    }
    if (!zr3a.isRs2Valid()) {
        cerr << "Re-read RS2 was not set to valid state (111...012)" << endl;
        return 1;
    }
    if (!zr3a.isRs2NotExpired()) {
        cerr << "Re-read RS2 expired (111...012)" << endl;
        return 1;
    }

    ZIDRecord zr5((unsigned char*) "210987654321");
    zid->getRecord(&zr5);


    // set new RS1 with expire interval of 5 second, then check immediatly
    zr5.setNewRs1((unsigned char*) "aaa22233344455566677788899900012", 5);
    if (!zr5.isValid()) {
        cerr << "Re-read ZID record '210987654321' not set to valid" << endl;
        return 1;
    }
    if (memcmp(zr5.getRs1(), "aaa22233344455566677788899900012", RS_LENGTH) != 0) {
        cerr << "RS1 (2) was not set (aaa...012)" << endl;
        return 1;
    }
    if (!zr5.isRs1Valid()) {
        cerr << "RS1 (2) was not set to valid state (aaa...012)" << endl;
        return 1;
    }
    if (!zr5.isRs1NotExpired()) {
        cerr << "RS1 (2) expired (aaa...012)" << endl;
        return 1;
    }

    // wait for 6 second, now the expire check shall fail
    sleep(6);
    if (zr5.isRs1NotExpired()) {
        cerr << "RS1 (2) is not expired after defined interval (aaa...012)" << endl;
        return 1;
    }

    zr5.setNewRs1((unsigned char*) "bbb99988877766655544433322211121", 256);
    zid->saveRecord(&zr5);

    zid->close();

    // Test migration
    zid->open("testzidOld");
    zid->close();

}

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */
