/*
 * Copyright (c) 2022 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.IntIntOrderedMap;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.random.WhiskerRandom;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.github.tommyettinger.ds.test.PileupTest.*;

/**
 * (This doesn't mark any problem multipliers.)
 * 0 problem multipliers in total, 65535 likely good multipliers in total.
 * Lowest collisions : 65119
 * Highest collisions: 339968825
 * Lowest pileup     : 9
 * Highest pileup    : 34
 * Testing a different set, with pileup calculated as the sum...
 * 0 problem multipliers in total, 16384 likely good multipliers in total.
 * Lowest collisions : 65521
 * Highest collisions: 716915767
 * Lowest pileup     : 9
 * Highest pileup    : 54060
 */
public class SmallCombinedHashTest {
	public static final int[] GOOD_MULTIPLIERS = {
			0x0014D8DB, 0x001882DF, 0x00100243, 0x001ED9D3, 0x00111431, 0x001CEEB9, 0x00191959, 0x001DE567,
			0x0012CB0D, 0x0010A3E1, 0x001D62B9, 0x001F09B3, 0x00101C75, 0x001E5E25, 0x0013A257, 0x001499D9,
			0x00149F6B, 0x00188E63, 0x0014AC37, 0x00118B0D, 0x0010C2AB, 0x0011DCA7, 0x001D316B, 0x00110C5D,
			0x0015CB9F, 0x001EA91F, 0x001C5381, 0x0013E159, 0x0012A5B7, 0x001E3027, 0x00119FC1, 0x001C45E1,
			0x0013B213, 0x001DCB35, 0x0019C0FD, 0x0017A10B, 0x001B67F7, 0x0018F0B3, 0x001CB79F, 0x00169B15,
			0x00109291, 0x00111F8F, 0x001657C9, 0x00193A65, 0x0017A771, 0x001C66B3, 0x001041A5, 0x0018E979,
			0x00191F25, 0x001E575F, 0x0015E537, 0x0017C1DD, 0x0019F5D5, 0x001BF421, 0x00179297, 0x001903E5,
			0x0012E75B, 0x0016D653, 0x001407BD, 0x001975A3, 0x001D5095, 0x001EC1E3, 0x00164FBB, 0x001315CD,
			0x0018EF0B, 0x001677C7, 0x001BB001, 0x001704C5, 0x0015A77D, 0x001CBC5D, 0x00111047, 0x0013369F,
			0x001D1A15, 0x001DDCBF, 0x00114C59, 0x00151345, 0x0011D957, 0x001548F1, 0x0016BA3F, 0x001C7F77,
			0x001896E5, 0x001F406D, 0x0010B1BB, 0x001E91C9, 0x001D5627, 0x00124B15, 0x00159909, 0x00137917,
			0x0014EA65, 0x001ECDDB, 0x001555BD, 0x001F6EDF, 0x001AF94F, 0x00184C5F, 0x001F82E5, 0x001243DB,
			0x001C2751, 0x0016ABCB, 0x001FE09D, 0x001D8AFF, 0x001EA4FB, 0x001F31F9, 0x00180F79, 0x0015CDE1,
			0x0013E39B, 0x001DC711, 0x001AC2CF, 0x0014849F, 0x001AD6D5, 0x001DEF1D, 0x0013B455, 0x001525A3,
			0x001CB37B, 0x0013A6B5, 0x0018F2F5, 0x00172A55, 0x00173E5B, 0x0013AD1B, 0x001B3AF3, 0x0019A7FF,
			0x0011C2D5, 0x0015BF6D, 0x0019BC05, 0x0018B60F, 0x0014407F, 0x0011F147, 0x001340EF, 0x0011784F,
			0x00153F3B, 0x001E3E61, 0x001120FD, 0x001A55CF, 0x00155341, 0x001A8B7B, 0x00138AA1, 0x0016FA3B,
			0x001B622B, 0x001FB615, 0x001ED1C5, 0x00115D0F, 0x001F946F, 0x00189395, 0x0011C867, 0x00161C51,
			0x00128B11, 0x00103573, 0x001DDFD5, 0x001714A7, 0x00121219, 0x001C2B3B, 0x001F0DD7, 0x001EEC31,
			0x0010EA7D, 0x001622B7, 0x0018AE01, 0x001F5789, 0x001856AF, 0x00148223, 0x0011D533, 0x001F7F95,
			0x001B9703, 0x001BCCAF, 0x00197711, 0x00102099, 0x0015E5D1, 0x0019ACBD, 0x00113A95, 0x001B3211,
			0x001E14AD, 0x00182DCF, 0x0015D831, 0x001A89D3, 0x001C30CD, 0x00152FF3, 0x0011F605, 0x0013F451,
			0x0016D6ED, 0x001FD613, 0x0015D0F7, 0x00108B1D, 0x0016645B, 0x00150713, 0x001B5949, 0x001E3BE5,
			0x00103A31, 0x0011AB7F, 0x00185507, 0x00196F03, 0x00194D5D, 0x0011468D, 0x001DFEFF, 0x0019E0C1,
			0x00153585, 0x001BBD67, 0x001046FD, 0x001AED1D, 0x001196A5, 0x00198FD5, 0x00118905, 0x0018BF8B,
			0x00104D63, 0x0017DB3B, 0x001FF611, 0x001DA073, 0x0015CF4F, 0x001FB2C5, 0x00141AB5, 0x00186E9F,
			0x001CF835, 0x001CA0E3, 0x00109D7B, 0x00174703, 0x001ED4DB, 0x001896AB, 0x0016CE0B, 0x001D7793,
			0x001D2041, 0x0014AE19, 0x0013C9C9, 0x001790B5, 0x001A1BFF, 0x0013A823, 0x001B6BA7, 0x0014A079,
			0x001364D7, 0x001C4257, 0x0017971B, 0x0017CCC7, 0x001F034D, 0x0018E6C3, 0x001D3AAD, 0x0016C6D1,
			0x0018C51D, 0x001419E1, 0x00122F9B, 0x0011D849, 0x00135D9D, 0x001D4113, 0x00164039, 0x001CE9C1,
			0x001A9423, 0x001EE80D, 0x001E03BD, 0x00131A51, 0x0012C2FF, 0x00104BBB, 0x001412A7, 0x001CF027,
			0x001FD2C3, 0x00125E0D, 0x001B19E7, 0x0019DE45, 0x0011A1C9, 0x001829AB, 0x00100135, 0x001D329F,
			0x00166771, 0x001C6255, 0x001CCDAD, 0x001219ED, 0x001C330F, 0x0011F847, 0x001D4D0B, 0x00173081,
			0x001E4561, 0x001043AD, 0x001D3F6B, 0x0013E8F3, 0x001CE819, 0x001171AF, 0x001F1C11, 0x001C17CF,
			0x0017F991, 0x001F0E71, 0x0014D3A9, 0x00139807, 0x00185749, 0x00105079, 0x001F14D7, 0x00144D11,
			0x0010BBD1, 0x001D81E3, 0x001E448D, 0x00165F63, 0x001E0141, 0x0012E229, 0x001A18AF, 0x001C2B01,
			0x001DD1FB, 0x001BFBBB, 0x0017DD7D, 0x0012B949, 0x001CD26B, 0x0015AFEB, 0x00167295, 0x001338A7,
			0x0018198F, 0x0012BFAF, 0x001FF119, 0x0015B651, 0x001F4275, 0x001EA7D7, 0x00121771, 0x001D6C35,
			0x001EDD83, 0x001E8631, 0x00152FB9, 0x0015F263, 0x00139CC5, 0x001EF189, 0x001C9BEB, 0x001E9A37,
			0x0018265B, 0x0015361F, 0x001818BB, 0x001CF9A3, 0x0015F8C9, 0x0011DA8B, 0x001B9C5B, 0x0019D3BB,
			0x001B4509, 0x001F0BF5, 0x001AEDB7, 0x0010B2EF, 0x0017C7CF, 0x001E1A05, 0x0013BD97, 0x0018F5D1,
			0x0015BBE3, 0x00157897, 0x001472A1, 0x0014FF9F, 0x001DFEC5, 0x001AC4D7, 0x00136CAB, 0x0019BEE1,
			0x0018B8EB, 0x0016F04B, 0x001F1F27, 0x001435BB, 0x0012D873, 0x00172C5D, 0x00102B83, 0x001DD5E5,
			0x001D7E93, 0x0017EF07, 0x001009DD, 0x001FB28B, 0x0012CAD3, 0x001A58AB, 0x001BA853, 0x0013362B,
			0x00178A15, 0x001FA4EB, 0x001EC09B, 0x00131485, 0x001CF7FB, 0x00162CCD, 0x0016B9CB, 0x001F0A4D,
			0x001279E7, 0x0016767F, 0x0013C98F, 0x001E2C63, 0x00189CD7, 0x001BB51F, 0x001419A7, 0x001E8A1B,
			0x0015C0A1, 0x001547A9, 0x001979ED, 0x001F3F25, 0x00105921, 0x00134FC3, 0x001F0FDF, 0x001E7541,
			0x001D399F, 0x001D6F4B, 0x001EE099, 0x0013C181, 0x001CC0A7, 0x001B8505, 0x001326E3, 0x00133AE9,
			0x001D6811, 0x00169CE3, 0x00102907, 0x001E6067, 0x001403F9, 0x00123B59, 0x001072B9, 0x0019FEDD,
			0x00138B01, 0x001FA26F, 0x00139F07, 0x001608AB, 0x001779F9, 0x001F94CF, 0x001A0543, 0x00155A07,
			0x0016A9AF, 0x0014E10F, 0x0016BDB5, 0x00146817, 0x001BF5EF, 0x00174AB3, 0x0015606D, 0x00162317,
			0x001C754D, 0x001F0097, 0x00168E6F, 0x0019A6B7, 0x0010503F, 0x0016F9C7, 0x001CBEFF, 0x001085EB,
			0x0018A0C1, 0x001346E1, 0x001BEEB5, 0x00197771, 0x001FC9A7, 0x001CA3BF, 0x001268F7, 0x001A97D3,
			0x001EEBBD, 0x001E076D, 0x001D231D, 0x00133207, 0x00144C03, 0x00192CEB, 0x001B2B37, 0x00154459,
			0x00165E55, 0x001C238D, 0x0018E99F, 0x001B74E9, 0x001756AB, 0x001316C7, 0x0010C129, 0x0013B7CB,
			0x00184161, 0x001CA951, 0x001A67B9, 0x001F9FF3, 0x0019B915, 0x001453EB, 0x00173687, 0x0017C385,
			0x001B8A71, 0x001F515D, 0x0019C1D1, 0x00151695, 0x001E15BB, 0x001BC01D, 0x001F8709, 0x0014BF43,
			0x00154C41, 0x001CDA19, 0x0016BD8F, 0x001E4B67, 0x001383A1, 0x00112E03, 0x001A2D29, 0x001DF415,
			0x0011BB01, 0x001B9E77, 0x001D0FC5, 0x0010D6B1, 0x001163AF, 0x001D4571, 0x0016D195, 0x001A9881,
			0x001E5F6D, 0x0019B431, 0x00105DB9, 0x001595F3, 0x001F2217, 0x00145A51, 0x001E3DC7, 0x00112063,
			0x0016589D, 0x001A1F89, 0x001AAC87, 0x00123A5F, 0x001C1DD5, 0x001F0071, 0x001071BF, 0x001AE233,
			0x0014C5A9, 0x001DC4CF, 0x001B6F31, 0x001F361D, 0x001CE07F, 0x001BFC2F, 0x001C892D, 0x0011C167,
			0x00158853, 0x0014A403, 0x00186AEF, 0x0019DC3D, 0x0013BFB3, 0x001B4D8B, 0x0018F7ED, 0x00128411,
			0x00192D99, 0x00184949, 0x00139E0D, 0x001764F9, 0x001827A3, 0x001FB57B, 0x00157AB3, 0x001D9589,
	};

//	public static final int[] GOOD_MULTIPLIERS = {
//			0x001006D1, 0x00104E61, 0x00105531, 0x00105981, 0x00108651, 0x0010A3E1, 0x00111431, 0x001138B1,
//			0x00119E61, 0x0011A3A1, 0x0011AE01, 0x00122151, 0x00127EC1, 0x00127F41, 0x0012ADA1, 0x0012B1E1,
//			0x0012BBD1, 0x0012EB71, 0x0012FB91, 0x001309E1, 0x00131911, 0x0013C481, 0x00140F71, 0x00142021,
//			0x00149261, 0x0014E021, 0x0015BE41, 0x0015F5E1, 0x00161871, 0x00161A41, 0x00167B41, 0x001686F1,
//			0x0016B0B1, 0x0016E381, 0x0016E9C1, 0x0016EC81, 0x0017BC01, 0x0017F0A1, 0x00184F31, 0x00185BB1,
//			0x00187FD1, 0x0018F781, 0x001928D1, 0x001938E1, 0x0019F771, 0x001B0E11, 0x001B3551, 0x001B8031,
//			0x001BB9A1, 0x001BBDC1, 0x001C0101, 0x001C52C1, 0x001C7531, 0x001CC121, 0x001D27B1, 0x001DD5C1,
//			0x001E3191, 0x001E4E91, 0x001ECCD1, 0x001EF321, 0x001FDD61, 0x00100141, 0x00100621, 0x001006E1,
//			0x001007D1, 0x00100851, 0x001009F1, 0x00100A21, 0x00100F11, 0x00101061, 0x00101461, 0x00101741,
//			0x00101A51, 0x00101CA1, 0x00101CF1, 0x00101F21, 0x00102261, 0x00102341, 0x00102601, 0x00102731,
//			0x001027D1, 0x00102981, 0x00102A61, 0x00102B01, 0x00103201, 0x00103211, 0x001033A1, 0x00103531,
//			0x00103A31, 0x00103B41, 0x00103E91, 0x00103FE1, 0x001041B1, 0x00104551, 0x001048A1, 0x001049F1,
//			0x00104AD1, 0x00104C11, 0x00104EF1, 0x001053B1, 0x00105811, 0x00105921, 0x00105AA1, 0x00105DE1,
//			0x00105F61, 0x00105FE1, 0x001061A1, 0x00106231, 0x00106401, 0x00106541, 0x00106861, 0x001069D1,
//			0x00106E21, 0x001070F1, 0x001073D1, 0x001074C1, 0x00108141, 0x001083B1, 0x00108491, 0x00108661,
//			0x00108961, 0x00108BC1, 0x00108CA1, 0x00109251, 0x00109291, 0x00109621, 0x00109881, 0x00109D91,
//			0x00109E31, 0x0010A2A1, 0x0010A311, 0x0010ABB1, 0x0010B1F1, 0x0010B311, 0x0010B631, 0x0010B891,
//			0x0010BAD1, 0x0010BBD1, 0x0010BEA1, 0x0010BF41, 0x0010C151, 0x0010C511, 0x0010C651, 0x0010C881,
//			0x0010C891, 0x0010C941, 0x0010CC01, 0x0010CC51, 0x0010CCB1, 0x0010CEA1, 0x0010D081, 0x0010D591,
//			0x0010DAE1, 0x0010DB41, 0x0010DC51, 0x0010DD11, 0x0010DD31, 0x0010DE51, 0x0010DE81, 0x0010DEE1,
//			0x0010E3B1, 0x0010E3E1, 0x0010E751, 0x0010E761, 0x0010E771, 0x0010E841, 0x0010E8C1, 0x0010EAE1,
//			0x0010ED81, 0x0010F1D1, 0x0010F271, 0x0010F3C1, 0x0010F6C1, 0x0010F741, 0x0010F7E1, 0x0010F931,
//			0x0010FAA1, 0x0010FE51, 0x0010FEB1, 0x001100B1, 0x00110151, 0x00110261, 0x001103B1, 0x00110521,
//			0x001105D1, 0x00110BC1, 0x00110C41, 0x00111721, 0x00111961, 0x00111AE1, 0x00111D71, 0x00111F71,
//			0x00112431, 0x001124B1, 0x00112721, 0x001127D1, 0x001128A1, 0x00112A91, 0x00112BA1, 0x00113381,
//			0x00113431, 0x00113561, 0x00113581, 0x00113601, 0x00113761, 0x001137C1, 0x00113A31, 0x00113A41,
//			0x00113C01, 0x00113C41, 0x00113D91, 0x00113E51, 0x00114211, 0x00114451, 0x00114501, 0x00114531,
//			0x001145C1, 0x00114691, 0x00114BB1, 0x00114D21, 0x00114EC1, 0x00115461, 0x001159D1, 0x00115A11,
//			0x00115AD1, 0x00115E51, 0x00116051, 0x00116A91, 0x00116B21, 0x00116EB1, 0x001170E1, 0x00117311,
//			0x00117901, 0x00117AF1, 0x00117BB1, 0x001180A1, 0x00118E91, 0x001190E1, 0x00119531, 0x00119C61,
//			0x00119E11, 0x00119FC1, 0x0011A141, 0x0011A1A1, 0x0011A2E1, 0x0011A411, 0x0011A611, 0x0011AF51,
//			0x0011B201, 0x0011B351, 0x0011B661, 0x0011B851, 0x0011BAD1, 0x0011BED1, 0x0011BF91, 0x0011C3C1,
//			0x0011C501, 0x0011C851, 0x0011CA91, 0x0011CD61, 0x0011CE61, 0x0011CEF1, 0x0011D081, 0x0011D511,
//			0x0011D5C1, 0x0011D881, 0x0011D8B1, 0x0011DA31, 0x0011DAF1, 0x0011DD51, 0x0011E1E1, 0x0011E361,
//			0x0011E5E1, 0x0011ED51, 0x0011F901, 0x0011F9F1, 0x0011FBA1, 0x0011FD01, 0x00120201, 0x001206A1,
//			0x00120931, 0x00120D91, 0x00121191, 0x00121221, 0x001213D1, 0x001213F1, 0x00121481, 0x00121501,
//			0x001216D1, 0x00121771, 0x00121AD1, 0x001224D1, 0x00122691, 0x001227E1, 0x001228D1, 0x001228F1,
//			0x00122AE1, 0x00122C51, 0x00122C91, 0x00122E11, 0x00123481, 0x00123671, 0x00123DB1, 0x001240E1,
//			0x001240F1, 0x00124101, 0x001241C1, 0x00124671, 0x00124861, 0x00124951, 0x001258C1, 0x00125B71,
//			0x00126021, 0x00126161, 0x00126261, 0x00126271, 0x00126671, 0x001266A1, 0x001266E1, 0x00126891,
//			0x00126911, 0x00126951, 0x001269F1, 0x00126DC1, 0x00127171, 0x001273A1, 0x00127541, 0x00127741,
//			0x001277F1, 0x00127831, 0x001279E1, 0x00127AF1, 0x00127B11, 0x00127B51, 0x00127EA1, 0x001280A1,
//			0x00128451, 0x00128461, 0x00128771, 0x00128881, 0x00128951, 0x00128B11, 0x00128CB1, 0x001293D1,
//			0x00129E71, 0x00129FD1, 0x0012A141, 0x0012A4F1, 0x0012A561, 0x0012A701, 0x0012ABA1, 0x0012ABE1,
//			0x0012ACC1, 0x0012AE41, 0x0012AE91, 0x0012AF21, 0x0012B091, 0x0012B541, 0x0012B681, 0x0012B831,
//			0x0012B9A1, 0x0012BD61, 0x0012C241, 0x0012C3F1, 0x0012C651, 0x0012C671, 0x0012C681, 0x0012C6F1,
//			0x0012C821, 0x0012C921, 0x0012C981, 0x0012C9A1, 0x0012CA11, 0x0012CC01, 0x0012CD71, 0x0012D101,
//			0x0012D121, 0x0012D241, 0x0012D461, 0x0012D5C1, 0x0012D6D1, 0x0012D821, 0x0012D9F1, 0x0012DD01,
//			0x0012E9B1, 0x0012EC61, 0x0012EFF1, 0x0012F1D1, 0x0012F2B1, 0x0012F3F1, 0x0012F591, 0x0012F5E1,
//			0x0012FA41, 0x0012FEF1, 0x0012FF41, 0x0012FFA1, 0x0012FFD1, 0x001301C1, 0x00130211, 0x00130541,
//			0x00130651, 0x001308F1, 0x00130A91, 0x00130B91, 0x00130C51, 0x00130C91, 0x00130D11, 0x00130F11,
//			0x00130F91, 0x001310D1, 0x001310E1, 0x00131231, 0x001317E1, 0x00131A21, 0x00131A51, 0x00131B41,
//			0x001322C1, 0x00132551, 0x00132571, 0x00132661, 0x00132CB1, 0x00132D21, 0x00132D41, 0x00132E01,
//			0x00133031, 0x001333D1, 0x00133971, 0x00133A51, 0x00133C11, 0x00133E11, 0x00134431, 0x00134461,
//			0x001346E1, 0x00134761, 0x00134EF1, 0x00134F51, 0x00135121, 0x00135171, 0x00135311, 0x001356E1,
//			0x00135E61, 0x00135F51, 0x00136621, 0x00136651, 0x00136D71, 0x001370D1, 0x00137191, 0x001372C1,
//			0x001378B1, 0x00137CB1, 0x00137E41, 0x00137EC1, 0x00137F91, 0x00138011, 0x001382B1, 0x00138491,
//			0x001385E1, 0x00138771, 0x00138A71, 0x00138AA1, 0x00138B01, 0x00138DE1, 0x00138F01, 0x00138F81,
//			0x001393B1, 0x00139521, 0x00139A51, 0x00139AD1, 0x00139C01, 0x00139C51, 0x0013A0A1, 0x0013A2D1,
//			0x0013A2F1, 0x0013A6C1, 0x0013A761, 0x0013AD21, 0x0013AE01, 0x0013AE41, 0x0013AEA1, 0x0013B011,
//			0x0013B191, 0x0013B311, 0x0013B501, 0x0013B861, 0x0013BA01, 0x0013BAD1, 0x0013C181, 0x0013C7D1,
//			0x0013CD11, 0x0013CFA1, 0x0013D501, 0x0013D541, 0x0013D631, 0x0013D781, 0x0013DEA1, 0x0013E431,
//			0x0013E631, 0x0013E8B1, 0x0013E981, 0x0013E9A1, 0x0013EC71, 0x0013EE61, 0x0013F081, 0x0013F451,
//			0x0013F4A1, 0x0013F501, 0x0013F631, 0x0013F691, 0x0013F831, 0x0013FB81, 0x0013FFC1, 0x00140281,
//	};

	public static final int LEN = 200000;
	public static final int COUNT = 1 << 14;
	public static final IntIntOrderedMap good = new IntIntOrderedMap(COUNT);

	static {
		int running = 1;
		for (int x = 0; x < COUNT; x++) {
			good.put((running = running + 0x9E376 & 0xFFFFF) | 0x100001, 0);
		}
	}
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		Collections.shuffle(words, rng);
        final Point2[] pointSpiral = generatePointSpiral(LEN);
		final Vector2[] vectorSpiral = generateVectorSpiral(LEN);

		final int[] problems = {0};
		for (int a = 0; a < COUNT; a++) {
			final int g = good.keyAt(a);
			{
				MetricSet pointSet = new MetricSet(g);
				MetricSet vectorSet = new MetricSet(g);
				MetricSet wordSet = new MetricSet(g);
				for (int i = 0, n = LEN; i < n; i++) {
					pointSet.add(pointSpiral[i]);
					vectorSet.add(vectorSpiral[i]);
				}
				for (int i = 0, n = words.size(); i < n; i++) {
					wordSet.add(words.get(i));
				}
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " P ");
				pointSet.clear();
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " V ");
				vectorSet.clear();
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " W ");
				wordSet.clear();
			}
		}
		good.sortByValue(IntComparators.NATURAL_COMPARATOR);

		System.out.println("\n\nint[] GOOD_MULTIPLIERS = new int[]{");
		for (int i = 0; i < Integer.highestOneBit(good.size()); i++) {
			System.out.print("0x"+Base.BASE16.unsigned(good.keyAt(i))+"=0x"+Base.BASE16.unsigned(good.getAt(i))+", ");
			if((i & 7) == 7)
				System.out.println();
		}
		System.out.println("};\n");
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + MetricSet.minMax[0]);
		System.out.println("Highest collisions: " + MetricSet.minMax[1]);
		System.out.println("Lowest pileup     : " + MetricSet.minMax[2]);
		System.out.println("Highest pileup    : " + MetricSet.minMax[3]);
	}

	private static class MetricSet extends ObjectSet {
		private final int initialMul;
        public static final long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		long collisionTotal;
		int longestPileup;
		int existingPileup;

		public MetricSet(int initialMul) {
			super(51, 0.6f);
			hashMultiplier = initialMul;
			this.initialMul = initialMul;
			collisionTotal = 0;
			longestPileup = 0;
			existingPileup = good.getOrDefault(initialMul, 0);
		}

		@Override
		protected int place (Object item) {
			return item.hashCode() * hashMultiplier >>> shift;
		}

		@Override
		protected void addResize (@NonNull Object key) {
			Object[] keyTable = this.keyTable;
			for (int i = place(key), p = 0; ; i = i + 1 & mask) {
				if (keyTable[i] == null) {
					keyTable[i] = key;
					return;
				} else {
					collisionTotal++;
					longestPileup = Math.max(longestPileup, ++p);
				}
			}
		}

		@Override
		protected void resize (int newSize) {
			int oldCapacity = keyTable.length;
			threshold = (int)(newSize * loadFactor);
			mask = newSize - 1;
			shift = BitConversion.countLeadingZeros(mask) + 32;

//						int index = (hm * shift >>> 5) & 511;
//						chosen[index]++;
//						hashMultiplier = hm = GOOD[index];
			Object[] oldKeyTable = keyTable;

			keyTable = new Object[newSize];

			longestPileup = 0;

			if (size > 0) {
				for (int i = 0; i < oldCapacity; i++) {
					Object key = oldKeyTable[i];
					if (key != null) {addResize(key);}
				}
			}
		}

		@Override
		public void clear () {
			good.put(initialMul, existingPileup + longestPileup);
			System.out.print(": Original 0x" + Base.BASE16.unsigned(initialMul) + " on latest " + Base.BASE16.unsigned(hashMultiplier));
			System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + good.get(initialMul));
			minMax[0] = Math.min(minMax[0], collisionTotal);
			minMax[1] = Math.max(minMax[1], collisionTotal);
			minMax[2] = Math.min(minMax[2], good.get(initialMul));
			minMax[3] = Math.max(minMax[3], good.get(initialMul));
			super.clear();
		}

		@Override
		public void setHashMultiplier (int hashMultiplier) {
			this.hashMultiplier = hashMultiplier | 1;
			resize(keyTable.length);
		}
	}
}