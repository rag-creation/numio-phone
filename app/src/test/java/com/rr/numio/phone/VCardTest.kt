package com.rr.numio.phone

import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactNumber
import com.rr.numio.phone.data.PhoneType
import com.rr.numio.phone.data.VCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardTest {
    @Test
    fun writesAndReadsBack() {
        val contacts = listOf(
            Contact(1, "k1", "Amma", listOf(ContactNumber("+919876543210", "Mobile", PhoneType.MOBILE)), false, null),
            Contact(
                2, "k2", "Office; Main",
                listOf(
                    ContactNumber("0501234567", "Work", PhoneType.WORK),
                    ContactNumber("0441234567", "Dubai SIM", PhoneType.CUSTOM),
                ),
                false, null,
            ),
        )
        val back = VCard.read(VCard.write(contacts))
        assertEquals(2, back.size)
        assertEquals("Amma", back[0].name)
        assertEquals("+919876543210", back[0].numbers.single().number)
        assertEquals(PhoneType.MOBILE, back[0].numbers.single().type)
        assertEquals("Office; Main", back[1].name)
        assertEquals(PhoneType.WORK, back[1].numbers[0].type)
        assertEquals(PhoneType.CUSTOM, back[1].numbers[1].type)
        assertEquals("Dubai SIM", back[1].numbers[1].label)
    }

    @Test
    fun readsOldAndroidQuotedPrintableNames() {
        // "അമ്മ" (Amma in Malayalam) as old Android phones export it, folded over two lines
        val vcf = "BEGIN:VCARD\r\nVERSION:2.1\r\n" +
            "FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=E0=B4=85=E0=B4=AE=E0=B5=8D=\r\n" +
            "=E0=B4=AE\r\n" +
            "TEL;CELL;PREF:+919876543210\r\nEND:VCARD\r\n"
        val back = VCard.read(vcf)
        assertEquals("അമ്മ", back.single().name)
        assertEquals(PhoneType.MOBILE, back.single().numbers.single().type)
    }

    @Test
    fun usesTheStructuredNameWhenThereIsNoFullName() {
        val vcf = "BEGIN:VCARD\nVERSION:3.0\nN:Nair;Ragav;;;\nTEL;TYPE=HOME:04 123 4567\nEND:VCARD\n"
        val back = VCard.read(vcf)
        assertEquals("Ragav Nair", back.single().name)
        assertEquals(PhoneType.HOME, back.single().numbers.single().type)
    }

    @Test
    fun readsVCard4TelUrisAndFoldedLines() {
        val vcf = "BEGIN:VCARD\nVERSION:4.0\nFN:Long\n  Name\nTEL;VALUE=uri;TYPE=cell:tel:+971501234567\nEND:VCARD\n"
        val back = VCard.read(vcf)
        assertEquals("Long Name", back.single().name)
        assertEquals("+971501234567", back.single().numbers.single().number)
    }

    @Test
    fun skipsContactsAlreadyOnThePhone() {
        val existing = listOf(Contact(1, "k", "Amma", listOf(ContactNumber("+919876543210", null)), false, null))
        val drafts = VCard.read(
            "BEGIN:VCARD\nFN:Amma\nTEL:09876543210\nEND:VCARD\nBEGIN:VCARD\nFN:Achan\nTEL:+919876500000\nEND:VCARD\n",
        )
        val fresh = VCard.newOnly(drafts, existing)
        assertEquals(listOf("Achan"), fresh.map { it.name })
        assertTrue(drafts.size == 2)
    }
}
