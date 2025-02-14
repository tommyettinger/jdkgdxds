/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.IntIntOrderedMap;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.random.WhiskerRandom;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.github.tommyettinger.ds.test.PileupTest.generatePointSpiral;
import static com.github.tommyettinger.ds.test.PileupTest.generateVectorSpiral;

/**
 */
public class IntCombinedHashTest {
	public static final int[] GOOD_MULTIPLIERS = {
//			0xE6AC8B4F, 0xC21736F9, 0xFD890F79, 0xC514D823, 0xF151575F, 0x8BDCE3EF, 0xA7F27B2F, 0x8C1EAA4F,
//			0xCCE4C43F, 0x82E28415, 0xC6A39455, 0xE6245E51, 0xC33AFB2F, 0xBFA927CB, 0xAC11C8A3, 0xC00E6AF1,
//			0xF01DF9A5, 0x9E7300F5, 0xA7E675F3, 0xFE3FB283, 0xE0FF1497, 0xB2CE9603, 0xD9EF3FCD, 0xB7F3D71B,
//			0xE438BEA9, 0xF16A6DCD, 0xA4613217, 0xAAE7C54B, 0xB56A208B, 0xBCA43B89, 0xEBE28BC7, 0x8567101B,
//			0xCD722469, 0xC3D56C8B, 0xEC34F7A1, 0xBF6C9497, 0xAE7244F5, 0xF3DEC4BF, 0xB79FD4FF, 0xFF54D7E7,
//			0xEE99B50F, 0xB965E897, 0xE37B5231, 0xF9FBF639, 0x965B209B, 0xFB164EC9, 0xE33667EF, 0x808498B7,
//			0x8E4EA9ED, 0xDB6B35F7, 0x9DCAF41B, 0xC2EEC5D5, 0xBBEB6F4F, 0x9645256D, 0x9BE82D2B, 0xC7C8533D,
//			0x8FA69905, 0xE4F8CD59, 0xAFDBF6C1, 0xC17E533D, 0x80922B93, 0xF40F52B7, 0xB824EF5F, 0x8EFBF295,
//			0xA495DEA3, 0xBF03ABE5, 0xFCE635CB, 0xB4422C7D, 0xCB39CDBD, 0xB370E565, 0x8701C045, 0xCD01F09F,
//			0xEA326ABB, 0x8DB96BBD, 0x97D3EA41, 0xE4B3EBB9, 0x8DCBB01B, 0xF3EDEDDD, 0xE018F82D, 0xE609C2E5,
//			0x8099F855, 0xCBC5273F, 0x978BDC1D, 0xEDEAD625, 0xB784AF47, 0x8C4C960F, 0xDC0CFB0D, 0xFCB95601,
//			0xA25AF703, 0xE0F6C62B, 0xB9229497, 0x80B41AB9, 0xCEA1EDDB, 0xB5A7B713, 0xC7AA1AE7, 0xED81746B,
//			0x83A0FC83, 0xD689CA89, 0xECE5E5D5, 0xC1CB8BEB, 0xF1D565C9, 0xC1ADCC9D, 0xB2985335, 0xF28FB701,
//			0xE8F1FBCF, 0xC8E7F03D, 0xF3AEC9B5, 0xCF030129, 0xB84DA7F3, 0x94A32A2B, 0xEB1A2609, 0xA1E0543F,
//			0xFAAD5D75, 0x9ED5A1E3, 0xBA56FE47, 0xFAB9E45B, 0x94DFAC1B, 0xC0A7D057, 0xC4E985F9, 0xA8612159,
//			0xE35C86ED, 0xFA0C22AB, 0xE607294F, 0xCC11653B, 0xFAD1EED3, 0xA49D3AFF, 0xB7D7B6B1, 0xC0A8E4EF,
//			0xCC8965C7, 0xA562FE85, 0xEE138EB9, 0xF0EE34C7, 0xEE739611, 0xFE191A7D, 0xCBBEE81D, 0xD0DE6BBD,
//			0xF6D38D31, 0x9F620FFD, 0x82E63243, 0xF4A1FFF3, 0xAD1185C7, 0xFCAE655D, 0x83B2BF37, 0xADE3D033,
//			0x9C2759F5, 0xA856B25F, 0xB1BEB50F, 0xCED724B3, 0xD68F0657, 0xC862D5DD, 0xACABAFB3, 0x9E01B893,
//			0x98FB4B61, 0x87D6B58B, 0xCE42682D, 0xC8FF67C7, 0x8F531893, 0xCFB5FEA7, 0x83D18A7B, 0xB07D3A2D,
//			0x8AE04AAD, 0xAC226E1D, 0x966E5FAB, 0xD241CD23, 0x84487F11, 0xA5DE22F3, 0xE2061CD3, 0xBE5B9F0B,
//			0xC88E2807, 0x8AEDE62B, 0x9A5DA973, 0xC1188E6D, 0xD08851B5, 0xA979EC49, 0xA26FD4F7, 0xFC92BDFB,
//			0xFF544D9B, 0x8D32BB1B, 0xCF2CB46F, 0xBCBECE53, 0xCF26F6F7, 0xE3C1B8CF, 0x92648319, 0xC29E60A3,
//			0x880E8E9B, 0xD7336DA5, 0xDF391243, 0xA2945DB3, 0xB943F971, 0x8009F5F3, 0x844BAB95, 0x8C515033,
//			0x8F009B75, 0x93A815E7, 0xC465778F, 0x9425DC8D, 0xCA564247, 0xDF3909A1, 0xC788231F, 0x854066D1,
//			0xAAA0CBBF, 0xF4E86F33, 0xD1D3B9E7, 0xE81D90D5, 0xFB9F0613, 0xCD3E83D3, 0xC3A1CBF5, 0xD4D8A629,
//			0xE935C8D7, 0xBF8F0567, 0x93912007, 0xCE810209, 0x8E0CE38D, 0xA898701D, 0xE6B6789F, 0x901BFFFB,
//			0xCA0A9FDB, 0xFC8F186F, 0xA8A4FFA5, 0xEA393429, 0xEF3AF87B, 0xB80947FD, 0xC2D190D3, 0xA8DBCCBF,
//			0x95B94A41, 0xD2403F83, 0xD3A1919F, 0x81668DEB, 0xEBA70489, 0xD0CDB4FF, 0xD81FD1D3, 0xB302C987,
//			0xABB672CD, 0x9E55CBF3, 0xCF5B331D, 0xA8FA9803, 0x8BD6AD6F, 0xD0ED94DB, 0xC3BCF1AD, 0xC30969E3,
//			0x8C2A253F, 0x88663643, 0xB8580E4B, 0x83657DE7, 0xE4E04137, 0xF9E0C7DF, 0xA68C7791, 0xC17DC8F1,
//			0xC9247ACF, 0xDC8DEE37, 0xA88A6439, 0x95E6BCF9, 0xDE0184B1, 0xE881C805, 0xCEA405C7, 0x9A3AB6F9,
//			0xE2C5A9D9, 0xC9FB76BD, 0xDA6C84C9, 0xB76FD153, 0xF3A9039B, 0xA49668EF, 0xE09F978B, 0xF701704F,
//			0xE39ECEF7, 0xEDD14F51, 0xB1E0228B, 0xE78D0427, 0x9438B3D9, 0xB5CE57BB, 0xABFBDEB9, 0x9CEC22C9,
//			0x8E2ED2B3, 0xB3A06767, 0xC179176F, 0x8605B14F, 0xD30A3B83, 0xDD8A7ED7, 0xB68E5DC9, 0xA00EC5B7,
//			0xB57AD749, 0xA0148BD1, 0xEB8D7DB5, 0xB5575A83, 0x8CE7A2FB, 0xA8D481A7, 0xF54EBE4F, 0xDFBF3899,
//			0xF458E5D9, 0x950540CD, 0xF13D03B7, 0xABDF22BF, 0xEF226C59, 0xACA4EEE7, 0x9D9532F7, 0xDB65787F,
//			0x86F7E439, 0xA3AA8F0D, 0xEFBF06E5, 0xCC448CC9, 0x95C66415, 0xACBE0555, 0xCC211003, 0xE72A6339,
//			0xCFCBE891, 0xFDA3293B, 0xDB41FBB9, 0xE86CE16F, 0xA94D8587, 0xA6D2E6D1, 0xCA179FC9, 0xDC9D87BB,
//			0xC14D4C3D, 0xF58C4C35, 0xFEF342EF, 0xB30ECEC3, 0xC4B12B3F, 0xE72A5A97, 0xF8CCB713, 0xF2406667,
//			0xA8FFC28D, 0x83887061, 0xCD0A47AD, 0xB672ADC5, 0xFAFF47A5, 0xA85E7F21, 0x816DD903, 0xCAA1ED55,
//			0x888A3D55, 0xCCFED55F, 0xA3F9555B, 0x93A04925, 0xC905253F, 0xAF7525FB, 0xC427E9A9, 0xA69E3A45,
//			0xEC6794E3, 0xFE09F15F, 0xAF2D17D7, 0xE0503E4F, 0xA9D2159B, 0xC38C5B13, 0x818CA447, 0xD510F841,
//			0xDEE379E5, 0xB9D1EC89, 0xF45C09BB, 0xA1858013, 0xE4B0C7D7, 0xC9A1B729, 0xF8B003BB, 0x9681B8A1,
//			0xC028736F, 0xA72E3CA7, 0xD20D17F5, 0xF1BDE59D, 0x981305C7, 0xD7AA623B, 0xF57C1721, 0xEE71FFCF,
//			0xD7FDEB4F, 0xFD6F8003, 0xB7C99989, 0xA50DF073, 0x87D20409, 0xC0950A4F, 0xD4944633, 0xF3AF4B5F,
//			0xB6570663, 0xDA7F4AD1, 0xBD0D9D43, 0xBE86F135, 0x95FF37D7, 0xBB230991, 0xCEA47ECF, 0xE87C8395,
//			0xA6E74BBD, 0xAE396891, 0x9097B757, 0xC8DCF6F7, 0x82E94D83, 0xB1A9D71B, 0xAC36D309, 0x8A6B6E03,
//			0x810B3815, 0xE1EA7571, 0xAFAE025F, 0xDBC3E995, 0x95B83E4B, 0xE6C1F38F, 0xD6E6ADFF, 0x86C08431,
//			0xCD48F2CD, 0xD8ACC197, 0x8EEF6BAF, 0xF201423F, 0xEF20DEB9, 0xF3E05B01, 0xC1A3E7EF, 0xD5732027,
//			0xAA4D42AB, 0xD5FD6511, 0xC9B04D59, 0x86798AA5, 0xFEF1ACAD, 0xC29ABB17, 0xF8655C01, 0x8BB6CD93,
//			0xF7CE9031, 0xBDBC7C2D, 0xCBFAF105, 0xA3557859, 0xF6D9CC53, 0xEFCFB501, 0x99D0C251, 0xD687BB3F,
//			0xFCEF1725, 0xD3360F57, 0x853CC145, 0xAE0C8025, 0xC02CA347, 0xBFDEE04D, 0xC2A18485, 0xD58D3147,
//			0xEC1E0A63, 0xD568B12D, 0x8F7507B9, 0xE0FC83A3, 0xE9E5B3B7, 0xA9B2D14F, 0xC7010217, 0xBF135F4F,
//			0xD6053A75, 0xA034DC13, 0xF6B19569, 0xE4C175F3, 0xAA43DFA7, 0xAD3C4503, 0x9131A709, 0x8D6DB80D,
//			0xD40E287F, 0xA1D1B56D, 0x99C65357, 0xBE1E9B71, 0x9268BB93, 0xAC58C22F, 0x92C8C2EB, 0xA3E79B49,
//			0xA0A16A51, 0xF3D7FB51, 0x91D9B3E3, 0xB51E6CDB, 0x94D2193F, 0xACCB0543, 0xC73E8FFD, 0x86A5E8C5,
//			0x9A62E541, 0xDE89BA51, 0xF831BB6B, 0x94545B3B, 0x94EA23B7, 0xC6BB1481, 0x967B797F, 0xE701AAA5,
//			0x92D5412F, 0xFAA69407, 0xD3927123, 0xFEDCCEB9, 0xDCF967DD, 0xE779AB31, 0xEF318CD5, 0x8AEE67D5,
//			0xDF2C09B3, 0xAE50E8BD, 0x996863EB, 0x8F182443, 0x829B08DF, 0xDDBF3CA7, 0xA01EFACB, 0xEA9C5F63,
//			0xEEFBCBB1, 0x85AB676F, 0x902BAAC3, 0x9B59B869, 0xD54E0D1F, 0x98F71B89, 0xFE7DD357, 0xC8E33619,
//			0xB483FB7F, 0xEE90520B, 0xC8D0FA5D, 0xCD12AFFF, 0xDDE982DB, 0x8426A12F, 0x911BC5C1, 0x8F36EF87,
//			0xFF7960A3, 0xADA421BF, 0x9EAC67A5, 0x89C3E2D3, 0x85B230DD, 0x8BBAFD6B, 0xEC04724B, 0xC7F42FB3,
//			0xC859FD25, 0xFDA6D769, 0xA4AF7F5D, 0xD0C56693, 0xB40740CF, 0xE8A09349, 0x9FB48479, 0xF80A9919,

			0x9B89CD59, 0xDCA1C8D7, 0xC5F768E7, 0x92317571, 0x937CD501, 0xE993C987, 0xD5567571, 0x85C8ADB5,
			0xE6AC8B4F, 0xC21736F9, 0xFD890F79, 0xC514D823, 0xF151575F, 0x8BDCE3EF, 0xA7F27B2F, 0x8C1EAA4F,
			0xCCE4C43F, 0x82E28415, 0xC6A39455, 0xE6245E51, 0xC33AFB2F, 0xBFA927CB, 0xAC11C8A3, 0xC00E6AF1,
			0xF98DDA5B, 0x8FA1F025, 0xF0CFFC71, 0xA49DC54B, 0xB3A7C3C3, 0xC2F9C7BD, 0xE0CC8899, 0xB3B0A51D,
			0xF01DF9A5, 0x9E7300F5, 0xA7E675F3, 0xFE3FB283, 0xE0FF1497, 0xB2CE9603, 0xD9EF3FCD, 0xB7F3D71B,
			0xE438BEA9, 0xF16A6DCD, 0xA4613217, 0xAAE7C54B, 0xB56A208B, 0xBCA43B89, 0xEBE28BC7, 0x8567101B,
			0x9F45E6F1, 0xF95D5505, 0xCDCBFDE3, 0xECFA5363, 0xB449917B, 0xE1D8CCA3, 0xE208D04F, 0xC0A33019,
			0xCD722469, 0xC3D56C8B, 0xEC34F7A1, 0xBF6C9497, 0xAE7244F5, 0xF3DEC4BF, 0xB79FD4FF, 0xFF54D7E7,
			0xEE99B50F, 0xB965E897, 0xE37B5231, 0xF9FBF639, 0x965B209B, 0xFB164EC9, 0xE33667EF, 0x808498B7,
			0xFFD4C7BD, 0xCF6740FB, 0x98EFE1B5, 0xA5CCFBCF, 0xB268E277, 0xFF48E3EF, 0xAB8ECED1, 0x83B4DFC5,
			0x8E4EA9ED, 0xDB6B35F7, 0x9DCAF41B, 0xC2EEC5D5, 0xBBEB6F4F, 0x9645256D, 0x9BE82D2B, 0xC7C8533D,
			0x8FA69905, 0xE4F8CD59, 0xAFDBF6C1, 0xC17E533D, 0x80922B93, 0xF40F52B7, 0xB824EF5F, 0x8EFBF295,
			0xFA482B93, 0x8492ADC3, 0xBCBA2E15, 0xD6F7F7AB, 0x80A58489, 0xB867400B, 0xD5D33021, 0xF43AAD83,
			0xA495DEA3, 0xBF03ABE5, 0xFCE635CB, 0xB4422C7D, 0xCB39CDBD, 0xB370E565, 0x8701C045, 0xCD01F09F,
			0xEA326ABB, 0x8DB96BBD, 0x97D3EA41, 0xE4B3EBB9, 0x8DCBB01B, 0xF3EDEDDD, 0xE018F82D, 0xE609C2E5,
			0xFE852FB3, 0xC2107ECF, 0xD42AE479, 0x9E128095, 0xF38831AF, 0xAF439D51, 0x87FF76C1, 0xD48BF7C7,
			0x8099F855, 0xCBC5273F, 0x978BDC1D, 0xEDEAD625, 0xB784AF47, 0x8C4C960F, 0xDC0CFB0D, 0xFCB95601,
			0xA25AF703, 0xE0F6C62B, 0xB9229497, 0x80B41AB9, 0xCEA1EDDB, 0xB5A7B713, 0xC7AA1AE7, 0xED81746B,
			0xAB08B11D, 0x8F63D7F3, 0x8FABDD75, 0x8F405B2D, 0xDD51AB15, 0xD7131D63, 0xFCC13CA9, 0xE7D8B7D7,
			0x83A0FC83, 0xD689CA89, 0xECE5E5D5, 0xC1CB8BEB, 0xF1D565C9, 0xC1ADCC9D, 0xB2985335, 0xF28FB701,
			0xE8F1FBCF, 0xC8E7F03D, 0xF3AEC9B5, 0xCF030129, 0xB84DA7F3, 0x94A32A2B, 0xEB1A2609, 0xA1E0543F,
			0xFF96253F, 0x87604B41, 0xA1FE1C2F, 0xD5FBE8B5, 0xF0707F65, 0xE81D17CD, 0x9D3430ED, 0xF44D7C2D,
			0xFAAD5D75, 0x9ED5A1E3, 0xBA56FE47, 0xFAB9E45B, 0x94DFAC1B, 0xC0A7D057, 0xC4E985F9, 0xA8612159,
			0xE35C86ED, 0xFA0C22AB, 0xE607294F, 0xCC11653B, 0xFAD1EED3, 0xA49D3AFF, 0xB7D7B6B1, 0xC0A8E4EF,
			0xAF255C59, 0xF101965F, 0xBA7DB033, 0x9AC1679B, 0xC1831603, 0xE6F4AAB7, 0x8DC1D40F, 0xC076504F,
			0xCC8965C7, 0xA562FE85, 0xEE138EB9, 0xF0EE34C7, 0xEE739611, 0xFE191A7D, 0xCBBEE81D, 0xD0DE6BBD,
			0xF6D38D31, 0x9F620FFD, 0x82E63243, 0xF4A1FFF3, 0xAD1185C7, 0xFCAE655D, 0x83B2BF37, 0xADE3D033,
			0x927B92DF, 0xF664655D, 0x9CB98587, 0x825DFCA3, 0xD5AC8F79, 0xC79860D5, 0xD063D19B, 0xDE0C7DF7,
			0x9C2759F5, 0xA856B25F, 0xB1BEB50F, 0xCED724B3, 0xD68F0657, 0xC862D5DD, 0xACABAFB3, 0x9E01B893,
			0x98FB4B61, 0x87D6B58B, 0xCE42682D, 0xC8FF67C7, 0x8F531893, 0xCFB5FEA7, 0x83D18A7B, 0xB07D3A2D,
			0xB423727D, 0xAA333A2D, 0x978F92ED, 0x830CD2EB, 0xA5EBC713, 0xD3C8C535, 0xDAE520E5, 0xCC356C4D,
			0x8AE04AAD, 0xAC226E1D, 0x966E5FAB, 0xD241CD23, 0x84487F11, 0xA5DE22F3, 0xE2061CD3, 0xBE5B9F0B,
			0xC88E2807, 0x8AEDE62B, 0x9A5DA973, 0xC1188E6D, 0xD08851B5, 0xA979EC49, 0xA26FD4F7, 0xFC92BDFB,
			0xFB73BC8B, 0xB26FABE5, 0xC2F8BBC7, 0xC5AA2797, 0xB5511B61, 0x8BCDD4B7, 0xC4431803, 0x853BC693,
			0xFF544D9B, 0x8D32BB1B, 0xCF2CB46F, 0xBCBECE53, 0xCF26F6F7, 0xE3C1B8CF, 0x92648319, 0xC29E60A3,
			0x880E8E9B, 0xD7336DA5, 0xDF391243, 0xA2945DB3, 0xB943F971, 0x8009F5F3, 0x844BAB95, 0x8C515033,
			0x9296AFF5, 0xA9288C65, 0xA0B1A807, 0xB725C529, 0xA36E8EC7, 0xA59B6A83, 0xB1173B23, 0x91F6787F,
			0x8F009B75, 0x93A815E7, 0xC465778F, 0x9425DC8D, 0xCA564247, 0xDF3909A1, 0xC788231F, 0x854066D1,
			0xAAA0CBBF, 0xF4E86F33, 0xD1D3B9E7, 0xE81D90D5, 0xFB9F0613, 0xCD3E83D3, 0xC3A1CBF5, 0xD4D8A629,
			0xB1A74627, 0x9DF00FC5, 0xC7559721, 0xAA19AAB7, 0xF0A31CA7, 0xBD355B25, 0xA3A55BE1, 0xF0855D59,
			0xE935C8D7, 0xBF8F0567, 0x93912007, 0xCE810209, 0x8E0CE38D, 0xA898701D, 0xE6B6789F, 0x901BFFFB,
			0xCA0A9FDB, 0xFC8F186F, 0xA8A4FFA5, 0xEA393429, 0xEF3AF87B, 0xB80947FD, 0xC2D190D3, 0xA8DBCCBF,
			0xF7A1B909, 0xA4CB2F61, 0xC5AD4B79, 0xF288FED7, 0xE45710E5, 0x8A2E6A69, 0xE69BEE77, 0xF0CE6ED1,
			0x95B94A41, 0xD2403F83, 0xD3A1919F, 0x81668DEB, 0xEBA70489, 0xD0CDB4FF, 0xD81FD1D3, 0xB302C987,
			0xABB672CD, 0x9E55CBF3, 0xCF5B331D, 0xA8FA9803, 0x8BD6AD6F, 0xD0ED94DB, 0xC3BCF1AD, 0xC30969E3,
			0x847FE7BB, 0xCB0959AB, 0x8CF7D80F, 0xFAEFB6C3, 0xD9EFDB5D, 0xA02B8A53, 0xCED41215, 0xDA322367,
			0x8C2A253F, 0x88663643, 0xB8580E4B, 0x83657DE7, 0xE4E04137, 0xF9E0C7DF, 0xA68C7791, 0xC17DC8F1,
			0xC9247ACF, 0xDC8DEE37, 0xA88A6439, 0x95E6BCF9, 0xDE0184B1, 0xE881C805, 0xCEA405C7, 0x9A3AB6F9,
			0xB520960B, 0xD83C14C5, 0xFF570117, 0x855FCDA5, 0xEB65580F, 0xB1A10705, 0xFE1B43AD, 0xDA0B0115,
			0xE2C5A9D9, 0xC9FB76BD, 0xDA6C84C9, 0xB76FD153, 0xF3A9039B, 0xA49668EF, 0xE09F978B, 0xF701704F,
			0xE39ECEF7, 0xEDD14F51, 0xB1E0228B, 0xE78D0427, 0x9438B3D9, 0xB5CE57BB, 0xABFBDEB9, 0x9CEC22C9,
			0x9BE73B1B, 0x897954FF, 0xC6E3D5B7, 0xE0D3DC53, 0xE79030AB, 0xA4E2AF8D, 0x86C337AD, 0xADCBDFA1,
			0x8E2ED2B3, 0xB3A06767, 0xC179176F, 0x8605B14F, 0xD30A3B83, 0xDD8A7ED7, 0xB68E5DC9, 0xA00EC5B7,
			0xB57AD749, 0xA0148BD1, 0xEB8D7DB5, 0xB5575A83, 0x8CE7A2FB, 0xA8D481A7, 0xF54EBE4F, 0xDFBF3899,
			0x8DAF8FB1, 0x83C514D9, 0xC5414787, 0xD5FA5B15, 0xD90AC247, 0x864C3A75, 0x828E004F, 0xC18F9447,
			0xF458E5D9, 0x950540CD, 0xF13D03B7, 0xABDF22BF, 0xEF226C59, 0xACA4EEE7, 0x9D9532F7, 0xDB65787F,
			0x86F7E439, 0xA3AA8F0D, 0xEFBF06E5, 0xCC448CC9, 0x95C66415, 0xACBE0555, 0xCC211003, 0xE72A6339,
			0x8699F4CD, 0xCAC0C9DD, 0xCA5B050D, 0xCA3D45BF, 0xE73DB38D, 0xAFA63E3F, 0xC94E3F59, 0xDAF09BD5,
			0xCFCBE891, 0xFDA3293B, 0xDB41FBB9, 0xE86CE16F, 0xA94D8587, 0xA6D2E6D1, 0xCA179FC9, 0xDC9D87BB,
			0xC14D4C3D, 0xF58C4C35, 0xFEF342EF, 0xB30ECEC3, 0xC4B12B3F, 0xE72A5A97, 0xF8CCB713, 0xF2406667,
			0xCC8EAA37, 0x87668A63, 0xFC988415, 0xCC0B2619, 0xBE0EF94B, 0xC75DEE2F, 0xFBDF3ED3, 0xF176FD55,
			0xA8FFC28D, 0x83887061, 0xCD0A47AD, 0xB672ADC5, 0xFAFF47A5, 0xA85E7F21, 0x816DD903, 0xCAA1ED55,
			0x888A3D55, 0xCCFED55F, 0xA3F9555B, 0x93A04925, 0xC905253F, 0xAF7525FB, 0xC427E9A9, 0xA69E3A45,
			0x84C86EE7, 0xAB058D3B, 0xB87E35EB, 0xA03786ED, 0x9AC482DB, 0xB6815DDB, 0xEFAA28F1, 0xC2E0B9F1,
			0xEC6794E3, 0xFE09F15F, 0xAF2D17D7, 0xE0503E4F, 0xA9D2159B, 0xC38C5B13, 0x818CA447, 0xD510F841,
			0xDEE379E5, 0xB9D1EC89, 0xF45C09BB, 0xA1858013, 0xE4B0C7D7, 0xC9A1B729, 0xF8B003BB, 0x9681B8A1,
			0xF845960B, 0xC0E3E1E1, 0xBDB5BB61, 0xBE338207, 0xDAD3E87D, 0xF47BE997, 0xF8D5A10F, 0xA7186401,
			0xC028736F, 0xA72E3CA7, 0xD20D17F5, 0xF1BDE59D, 0x981305C7, 0xD7AA623B, 0xF57C1721, 0xEE71FFCF,
			0xD7FDEB4F, 0xFD6F8003, 0xB7C99989, 0xA50DF073, 0x87D20409, 0xC0950A4F, 0xD4944633, 0xF3AF4B5F,
			0xC9F75829, 0xF83A1B1B, 0xE5CC34FF, 0xC57FE163, 0x9BFC9AB9, 0xFC2E0DC3, 0xF50BF49B, 0xF5A1BD17,
			0xB6570663, 0xDA7F4AD1, 0xBD0D9D43, 0xBE86F135, 0x95FF37D7, 0xBB230991, 0xCEA47ECF, 0xE87C8395,
			0xA6E74BBD, 0xAE396891, 0x9097B757, 0xC8DCF6F7, 0x82E94D83, 0xB1A9D71B, 0xAC36D309, 0x8A6B6E03,
			0x85AD0653, 0xAB369CDD, 0x964E180B, 0xB23AF6B7, 0xC884CDA5, 0xD0F03713, 0xB41A0F79, 0xF34BA71D,
			0x810B3815, 0xE1EA7571, 0xAFAE025F, 0xDBC3E995, 0x95B83E4B, 0xE6C1F38F, 0xD6E6ADFF, 0x86C08431,
			0xCD48F2CD, 0xD8ACC197, 0x8EEF6BAF, 0xF201423F, 0xEF20DEB9, 0xF3E05B01, 0xC1A3E7EF, 0xD5732027,
			0xA7CCF7C1, 0xCA462719, 0xD0193283, 0xB8B05183, 0xEA393CCB, 0xEA513EA1, 0x9308FBAB, 0xE985B501,
			0xAA4D42AB, 0xD5FD6511, 0xC9B04D59, 0x86798AA5, 0xFEF1ACAD, 0xC29ABB17, 0xF8655C01, 0x8BB6CD93,
			0xF7CE9031, 0xBDBC7C2D, 0xCBFAF105, 0xA3557859, 0xF6D9CC53, 0xEFCFB501, 0x99D0C251, 0xD687BB3F,
			0x8E13BE3F, 0xA6D833E3, 0xD2E85DA1, 0xA505A207, 0xE27022BF, 0xA23D4057, 0xC54C382B, 0x88A7839B,
			0xFCEF1725, 0xD3360F57, 0x853CC145, 0xAE0C8025, 0xC02CA347, 0xBFDEE04D, 0xC2A18485, 0xD58D3147,
			0xEC1E0A63, 0xD568B12D, 0x8F7507B9, 0xE0FC83A3, 0xE9E5B3B7, 0xA9B2D14F, 0xC7010217, 0xBF135F4F,
			0xBBDF83F9, 0xB58336F9, 0xE3ADF815, 0xFF1D101B, 0xF54A9719, 0xB8101A0D, 0xC2C0610D, 0xDA6B8A17,
			0xD6053A75, 0xA034DC13, 0xF6B19569, 0xE4C175F3, 0xAA43DFA7, 0xAD3C4503, 0x9131A709, 0x8D6DB80D,
			0xD40E287F, 0xA1D1B56D, 0x99C65357, 0xBE1E9B71, 0x9268BB93, 0xAC58C22F, 0x92C8C2EB, 0xA3E79B49,
			0xA0597613, 0xF312406D, 0xAF8133D9, 0x8CE487BB, 0xD82D75F3, 0xADDEA5A9, 0xB8112603, 0xDBCDE829,
			0xA0A16A51, 0xF3D7FB51, 0x91D9B3E3, 0xB51E6CDB, 0x94D2193F, 0xACCB0543, 0xC73E8FFD, 0x86A5E8C5,
			0x9A62E541, 0xDE89BA51, 0xF831BB6B, 0x94545B3B, 0x94EA23B7, 0xC6BB1481, 0x967B797F, 0xE701AAA5,
			0xD3F84737, 0xC2502D43, 0xE266BFBB, 0xB30AB02F, 0xBD8AF383, 0xB322B205, 0xDD23BF55, 0xF67DFD75,
			0x92D5412F, 0xFAA69407, 0xD3927123, 0xFEDCCEB9, 0xDCF967DD, 0xE779AB31, 0xEF318CD5, 0x8AEE67D5,
			0xDF2C09B3, 0xAE50E8BD, 0x996863EB, 0x8F182443, 0x829B08DF, 0xDDBF3CA7, 0xA01EFACB, 0xEA9C5F63,
			0xA69FD929, 0xBBA05FD1, 0x965ECEC9, 0xA5EC515F, 0xD9D21C0F, 0xFA18B233, 0xD9A8E1D1, 0xC7053A91,
			0xEEFBCBB1, 0x85AB676F, 0x902BAAC3, 0x9B59B869, 0xD54E0D1F, 0x98F71B89, 0xFE7DD357, 0xC8E33619,
			0xB483FB7F, 0xEE90520B, 0xC8D0FA5D, 0xCD12AFFF, 0xDDE982DB, 0x8426A12F, 0x911BC5C1, 0x8F36EF87,
			0xBC1F326D, 0x92B3ED99, 0xBF479B75, 0xDA332F5D, 0xDBAC834F, 0xB69AF5F3, 0xD9A9EDC7, 0xC443A24F,
			0xFF7960A3, 0xADA421BF, 0x9EAC67A5, 0x89C3E2D3, 0x85B230DD, 0x8BBAFD6B, 0xEC04724B, 0xC7F42FB3,
			0xC859FD25, 0xFDA6D769, 0xA4AF7F5D, 0xD0C56693, 0xB40740CF, 0xE8A09349, 0x9FB48479, 0xF80A9919,
	};

	public static final int STEP = 2;
	public static final int LEN = 200000;
	public static final int COUNT = 1 << 16;
	public static final IntIntOrderedMap good = new IntIntOrderedMap(COUNT);

	static {
		int running = 1 + 0x9E3779B6 * COUNT * STEP;
		for (int x = 0; x < COUNT; x++) {
			good.put((running = running + 0x9E3779B6) | 0x80000001, 0);
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
				for (int i = 0, n = LEN; i < n; i++) {
					pointSet.add(pointSpiral[i]);
				}
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " P ");
				pointSet.clear();
				MetricSet vectorSet = new MetricSet(g);
				for (int i = 0, n = LEN; i < n; i++) {
					vectorSet.add(vectorSpiral[i]);
				}
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " V ");
				vectorSet.clear();
				MetricSet wordSet = new MetricSet(g);
				for (int i = 0, n = words.size(); i < n; i++) {
					wordSet.add(words.get(i));
				}
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " W ");
				wordSet.clear();
			}
		}
		good.sortByValue(IntComparators.NATURAL_COMPARATOR);
		final long lowest = good.getAt(0), highest = good.getAt(good.size()-1);
		good.truncate(2048);
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
		System.out.println("Lowest pileup     : " + lowest);
		System.out.println("Highest pileup    : " + highest);
	}

	private static class MetricSet extends ObjectSet {
		private final int initialMul;
        public static final long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE};
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
		protected int place (@NonNull Object item) {
			return BitConversion.imul(item.hashCode(), hashMultiplier) >>> shift;
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
			super.clear();
		}

		@Override
		public void setHashMultiplier (int hashMultiplier) {
			this.hashMultiplier = hashMultiplier | 1;
			resize(keyTable.length);
		}
	}
}
