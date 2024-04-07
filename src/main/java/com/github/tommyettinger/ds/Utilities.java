/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.digital.Hasher;

import java.util.Arrays;

import static com.github.tommyettinger.digital.Hasher.*;

/**
 * Utility code shared by various data structures in this package.
 *
 * @author Tommy Ettinger
 */
public final class Utilities {
	/**
	 * A final array of 512 long multipliers that have been tested to work on at least some large
	 * input sets without excessively high collision rates. The initial value passed to a
	 * {@link ObjectSet#setHashMultiplier(long)} method (on any hashed data structure here) is used
	 * to choose one of these based on that long value (actually 11 of its middle bits). All hashed
	 * data structures here currently start with a multiplier of 0xD1B54A32D192ED03L, which is not
	 * in this array by default, but is still a pretty good multiplier.
	 * <br>
	 * You can mutate this array, but you should only do so if you encounter high collision rates or
	 * resizes with a particular multiplier from this table. Any long you set into this array must
	 * be an odd number, should be very large (typically the most significant bit is set), and should
	 * ideally have an "unpredictable" bit pattern. This last quality is the hardest to define, but
	 * generally dividing 2 to the 64 by an irrational number using BigDecimal math gets such an
	 * unpredictable pattern. Not all irrational numbers work; some are pathologically non-random
	 * when viewed as bits, but usually that only happens when the number was specifically constructed
	 * as a counter-example or an oddity. As always, <a href="http://oeis.org/search?q=decimal+expansion">The OEIS</a>
	 * proves a useful resource for getting long sequences of irrational digits, and if you make a
	 * BigDecimal with about 25 irrational digits, and divide (or multiply) it by 2 to the 64, convert
	 * it to a {@code long}, and if even, add 1, you'll have a good number to substitute in here...
	 * most of the time. Using the golden ratio (phi) does startlingly poorly here, even though
	 * mathematically it should be the absolute best multiplier.
	 * <br>
	 * The specific numbers in this array were chosen because they performed well (that is, without
	 * ever colliding more than about 25% over the average) on a data set of 200000 Vector2
	 * objects. Vector2 is from libGDX, and it can have an extremely challenging hashCode() for hashed
	 * data structures to handle if they don't adequately mix the hash's bits. On this data set, no
	 * multiplier used recorded fewer than 600000 collisions, and any multipliers with collision counts
	 * above a threshold of 677849 were rejected. That means these 512 are about as good as it gets.
	 * The multipliers were also tested on a similar data set of 235970 English dictionary words
	 * (including many proper nouns), with fewer collisions here (a little more than 1/20 as many).
	 * All multipliers satisfied a threshold of 40000 collisions with the dictionary data set.
	 */
	public static final long[] GOOD_MULTIPLIERS = new long[]{
		0xABC98388FB8FAC03L, 0x89E182857D9ED689L, 0xC6D1D6C8ED0C9631L, 0xAF36D01EF7518DBBL,
		0x9A69443F36F710E7L, 0xE60E2B722B53AEEBL, 0xB0C8AC50F0EDEF5DL, 0x92E852C80D153DB3L,
		0xEBEDEED9D803C815L, 0xB8ACD90C142FE10BL, 0x908E3D2C82567A73L, 0x8538ECB5BD456EA3L,
		0xEDF84ED4185625ADL, 0xDD35B2CD88449739L, 0xBF25C1FA63435003L, 0x9989A7D8994B239DL,
		0x8EB95D05457AA16BL, 0xC48CA286CD4C4B4DL, 0xAC38B6695442DF75L, 0x96E7A62094FC418FL,
		0xE2E8D1BC93B1BCBFL, 0xD5A0DBC4254CF233L, 0xC91FE60DC666F779L, 0x9E031B3B27A07ECDL,
		0x8C0E724FD5446177L, 0x83DBDF3EF6A3B35BL, 0xE51CC09B3D83CAFFL, 0xCD0C73D06EEC786BL,
		0xAD9BA24D9CF0D513L, 0x92FCF6CA403AB5A5L, 0x838CCB04C5269A59L, 0xDB70396E42C39DCBL,
		0xBC193374F397645DL, 0xA9BBB6A090C6F39DL, 0xA13C043E2FE475FBL, 0x992942A852DFBF6FL,
		0xDDC8F72B7958B04FL, 0xC98E188E5138882DL, 0xC02498843215A687L, 0xB72B9CF7CB66736FL,
		0x9036EA018B2BCF9FL, 0x897AE4FC66EB83E9L, 0xF4CCD627D640C91BL, 0xEA171C21F30F9703L,
		0xCCB0DCDF5F586A6BL, 0xC3BC5AB09BC44545L, 0xA3AA5A3471A8B1F5L, 0x95A87CBE60D27129L,
		0xF57716BC263D1509L, 0xD8642B04DD8237EDL, 0xCF7C86F34CD519B3L, 0xBEC2D147B3AAF99BL,
		0xB6E92FC9D8D36E9DL, 0xAF62415FDC02D5B3L, 0x943F870341597D83L, 0x8E25C2E84A744115L,
		0x82B0645B06A50EEBL, 0xEC7F64E5DBDB09E5L, 0xE34F959426E19C8FL, 0xC9D63C0265C9E5E3L,
		0xC1FF1A4B21B8D3A7L, 0xBA75F026E4723465L, 0xB337B641246F3469L, 0x98F3A45E9392C40DL,
		0x87CF974DE8CBB6C7L, 0x82890B01154E248BL, 0xE4C6DA7D4C75AFC7L, 0xDC5C91B62A616C9DL,
		0xD44186D34B52C711L, 0xBDAF3BEA26B5C657L, 0xB6B5103A2FC143B5L, 0xAFFC9813B30F3AFBL,
		0x9D45AB02671CFAD7L, 0x8265F322AF35A6BFL, 0xC09FF697B9FEC8F7L, 0xB9E58585A5D89199L,
		0xAD23075AFC31BA73L, 0xA140BE90E0821C0BL, 0xE74D154856960573L, 0xD82C7821AF16CE8DL,
		0xD0FC339E5B8BC6BBL, 0xC35137BF42996A17L, 0xBCD27FCF29165CF9L, 0xB68B12725B6A44F9L,
		0xA4EE74A6BA40A66FL, 0x90103F13887A534BL, 0x86A43AC3D2D7E5D9L, 0xE86546245CFD180FL,
		0xBF83F38E96F3936DL, 0xB9703D7E0096EF81L, 0xADDB584198C64BF5L, 0xA85713D17B33C9EDL,
		0x9DD38F27F07609E7L, 0x98D18270621C5493L, 0x93F8240566E5E78DL, 0x86536A15F9DFF487L,
		0x8210437F25824997L, 0xF0B35A2524B90EF9L, 0xDB72DC3546A1A6C1L, 0xC200A49AD6AFC313L,
		0xAB81C8139ABA1463L, 0x9304F97B17392461L, 0x8E8EFE9E6C844FD5L, 0xF88EE8EC947BAC23L,
		0xE381868217F635A9L, 0xBE997F955D5F55F7L, 0xAE74CE8EBF5EE175L, 0x9FAE24F434F06829L,
		0x8DE8356B76B8A95BL, 0x89C82BAB51682E05L, 0xE49ACC3BA426BE8FL, 0xC672152B60A84E73L,
		0xC0E8BE65379A4499L, 0xBB86F28B24601C5BL, 0xB13599CC82969B59L, 0x9E3EE498A8DF438DL,
		0x8D4FB3AC765D01CDL, 0x895E6D7A1699DA71L, 0x85894FB89C101161L, 0x81CF914BE11C4529L,
		0xF92258D8434F8A31L, 0xD9786511656FDABFL, 0xD3A347C4717A9A2BL, 0xCDF635F0848CE469L,
		0xA1428CE6F71ABD83L, 0x98B9E08AD13C30D7L, 0xF963D37E6AD397B1L, 0xECAD6DC073812739L,
		0xE690FA59735A5CA5L, 0xE09CEC0D2BB40BEFL, 0xDAD037D85A1FAFE9L, 0xD529D99CB4258D85L,
		0xC512FD479630A193L, 0xBFFC51828C1BCC49L, 0xA872761A61E09887L, 0xA419044CDB5FCCEDL,
		0x9FDC526D126E78A5L, 0x97B63B38C5CE8A63L, 0x93CB68693D2A2E11L, 0x8C42C5C9000F11A1L,
		0xF369D04BE1B34389L, 0xED5AA0254466E043L, 0xE7720D2C9634DC87L, 0xE1AF214F0C24C34FL,
		0xD6968513E24AD1C3L, 0xD13F06963376BC15L, 0xCC0992A85C5E894FL, 0xC6F5505B81C0B31DL,
		0xB3DFFEFD76CFF10DL, 0xA29F4217F711A191L, 0x930627ED77C26777L, 0x8BCB9D0D5DF1A803L,
		0x8850BF1A217BFC29L, 0x819CFD6E329C1075L, 0xF9D929986B5EEEB9L, 0xF3D82B220C06E07BL,
		0xEDFC1BCF7B16B91BL, 0xE844186B738DCE55L, 0xD2BB7E5FBCDABC13L, 0xC3E71B73995679F7L,
		0xA94D02ACF6FE122DL, 0xA53B8563A303BF93L, 0xA1430F0AE4052279L, 0x924FAAD3D6C3FB33L,
		0x8B5D2B060EBF2C2DL, 0x84BF1DEE3C41BFEBL, 0xE90890F198E9682FL, 0xDE55B58EA47EDAA5L,
		0xCF33602223E1A33DL, 0xC118C39576C84389L, 0xBC9CB16A84ED9C1DL, 0xABB0ED29A2485B47L,
		0xA7B41FC6A7BAD15BL, 0xA3CF07A52930C135L, 0x98A88D80637BC8CFL, 0x951CE92C9BB209A9L,
		0x91A6594C9E8D459BL, 0x8AF6847D28037703L, 0xE9C0BDF597E5D9BFL, 0xDF5D7E301FAC5725L,
		0xDA5884709D33B639L, 0xD5706AE68BD8CD09L, 0xD0A48B7403BDE0F3L, 0xC75EF4F2A5961C7DL,
		0xBE82D926D2A51FD9L, 0xB1F443415B14DE55L, 0xAA0BCC5F7EB08975L, 0xA27D4C74FAECC655L,
		0x9108D81075AC1E93L, 0x8A96E23DFBE9515DL, 0x817466086C7CD82DL, 0xFA6D85BD7B83D8BFL,
		0xF4FA18113E772031L, 0xEFA509F857AEFD4BL, 0xE05593038E3D2B9BL, 0xD6ACB8C8173F6B75L,
		0xD2008424B4FDC1ABL, 0xC495DDE12DB49653L, 0xAC44D8CC7D2631A7L, 0xA884EF0747DCAEFDL,
		0xA4D9EA10ADE9061BL, 0x9DC0BF6602AF8781L, 0x9A51B86503E8B7EFL, 0x96F5D37CD66A4963L,
		0x8D50D2C3F1EC6CD1L, 0xE13F5115FBAFF277L, 0xD349104A395B3CA7L, 0xCED39350D14AFF63L,
		0xCA762D55332BD5AFL, 0xBDE97D88B6B90CF3L, 0xB5FB1C0DFD25ABBBL, 0xAE6185B4A5EA78B3L,
		0xA7192FF23CD39E85L, 0xA3926C63B2E2553DL, 0x996ED7BF82078107L, 0x87005E7FA89E04BBL,
		0x815D0BFBCE78BCDBL, 0xF59F42F9E9A5D9DDL, 0xF0978EA6AB84D035L, 0xEBAA39469D2D7F79L,
		0xDD7B1A6BD60CA405L, 0xD02687CC29263023L, 0xC7B65E59DDA5BE8DL, 0xBBB1344F1532941BL,
		0xB7D93465D7A27B45L, 0x957C2656AF96B259L, 0x8F6CCFE8B5F4C1FFL, 0x8C7CE37811AB45E7L,
		0xF5EA757A0A98C863L, 0xD165EB8B5BC1E6E5L, 0xC92651AD1416562FL, 0xC525EB170F87E511L,
		0xC139E57FB4584EF5L, 0xBD61D91E459DF78BL, 0xB5EC17226EFEF75BL, 0xB24D9C1F6CBCA9C1L,
		0xA1427248B4D5B15FL, 0x97D3616F4A74FC6BL, 0x94CE2C6AFED5D56BL, 0xECC3700594D89347L,
		0xE82F3D74BAE83EF3L, 0xE3B1B6599529F247L, 0xDAF8EBBE23F4F3D7L, 0xD295A81B326269ABL,
		0xCA84A4C0884C271DL, 0xC2C2BB1E3012F58FL, 0xBEFE7C603145DEB5L, 0xB7AD9446AE16AAD9L,
		0xA697335663D548B5L, 0xA35E6C53DF056371L, 0x9D1C699098755D61L, 0x9A129165871CCFF3L,
		0x8662F6192BF6EFA7L, 0xFB2E70F0672DEAC3L, 0xE01DABDDB18AAEF3L, 0xCBCD89D79297A36BL,
		0xC7F77EFE709ACF33L, 0xC433F0345A4ED6BFL, 0xC082846776CFB603L, 0xBCE2E4331DF57083L,
		0xB954B9D7C43C7F5DL, 0xB5D7B1330D9B8881L, 0xABC42FC89F4EBF83L, 0xA55C6C2FB54D7F91L,
		0x99420F2A06C5E287L, 0x965F9259C086E847L, 0x8E0A780F8D0B4DFBL, 0x8B5E0842C9AC7A2DL,
		0x88BE7A7AFBAD244BL, 0xDCD6EE4E25C47F99L, 0xD4D4E48248F14DF5L, 0xD0EFEBB90F18CC85L,
		0xC5AD21EE97860B19L, 0xC20F273116063D41L, 0xBB05B7197B4187B7L, 0xB799A475E04D3A53L,
		0xADB47064790FFB1BL, 0xAA86BFE6B51FC425L, 0xA767F2FE45A47E49L, 0xA457C3EC7B873C09L,
		0x9B7C434D006FE831L, 0x931AF84C032207B3L, 0x8DC566B8C4F9DBF5L, 0x88A1599FE405FD95L,
		0x8144356D4D794CB5L, 0xF6EF948D7C022F77L, 0xF2865D3AD18E3E67L, 0xEE3151B6A0B5A4E3L,
		0xE9F015C346AAD57BL, 0xE1A7A3CE0E5A4DD5L, 0xD9AA45DB9081A661L, 0xD5C6E8C646A11777L,
		0xD1F55366F1743DEDL, 0xC6E81BB754E182C5L, 0xBFDD30C029F0535DL, 0xB9121BAF15A7D079L,
		0xB5C3CBBC9C1C2DA1L, 0xAF54410C19C57735L, 0xA91F0BE7F700F123L, 0xA619AB7160EF7EB9L,
		0xA0381D18A5B676E5L, 0x9D5B71FE680B6AC1L, 0x97C92339B9FC9B9FL, 0x951308EE89759779L,
		0x8D3A3CB2556329E7L, 0x8AB46A87D8652177L, 0x883A21A5C11EF379L, 0x85CB2D4AF4E9FD39L,
		0xFB8F8AF296473783L, 0xEA8F6605FAB5E921L, 0xE67E1E716C665867L, 0xDAB55FAF0492CFAFL,
		0xBAD967768509DE5DL, 0xB46CD56B5C0CECC5L, 0xB14BDFF91E10ACD5L, 0xA83B602DAB115255L,
		0x9A2482200E1CFE17L, 0x94D7CEE233BE3A55L, 0x92430D2DB9FDC787L, 0x8FB9C0C2F03FCCE9L,
		0x8AC8BD356B173707L, 0xFBAD8DEED8FB2209L, 0xF340636968788E7FL, 0xDF662F9D37E0C88DL,
		0xD7EB66B5ABFF26C7L, 0xD4462F7EA3D6623FL, 0xC2F32FBDE17D3F97L, 0xB93DDED17D45DFA9L,
		0xB00451E7C16C6AC5L, 0x9C3D4C3F967EA489L, 0x999A05CE67EFC51DL, 0x94757B5F75232DCBL,
		0x8AAF297359B40FA7L, 0x8857C2E6FA2012D3L, 0x818D98E40D2D198FL, 0xF78976F1379136DFL,
		0xF3693491C2390A33L, 0xEB5D361D9D81532BL, 0xE3955173459932CDL, 0xCDC0196F1CDB6263L,
		0xC6F2D3F03B88419BL, 0xC05F1EDF98799989L, 0xBD2A41A08F91F0EDL, 0xB0DD5C8FC9D731F7L,
		0xAB048CF1F423F1F3L, 0xA82ACDF91DEED89DL, 0xA29B99C5C5EFC9ADL, 0x9A9C862CFDBD36ABL,
		0x95800B0F0BA7BBF1L, 0x8BC76BE0AD7F873DL, 0x84E7BCE59BC138E5L, 0x8082F505AC3B9F93L,
		0xFBE7BD0A608BAD17L, 0xF7E03E790DA47477L, 0xF3E93FA3A9F8B32BL, 0xE864A3559C6B8123L,
		0xE104AA5CC856ED81L, 0xDD6B446842F7AE65L, 0xD9E09B5E1EEFF399L, 0xCF96B71150EFF3CDL,
		0xCC44AFC7DA5003D5L, 0xC900412A8DE11143L, 0xBF82610E450F2C2FL, 0xBC723139968EE63BL,
		0xB38C17810B44A553L, 0xB0ACE30A700728ADL, 0xA2FD8A945ECE591BL, 0x9DD16654E362103BL,
		0x9B4B2BD28FC9A05FL, 0x965D8EF42464A387L, 0x8AB7EB3C75E31201L, 0x842AF82B7006ABD1L,
		0xF7F6DC82CD3A64A7L, 0xE8A44647F724B981L, 0xD6D0207D002D7BBFL, 0xD011F460711BA35BL,
		0xC989F7439FBD2BEBL, 0xBD15CA175AF5D381L, 0xBA18044CA28AC8EDL, 0xB7265BCC1186DD6FL,
		0xAE982B47045E59F9L, 0xA6704A0BB82AFE25L, 0x94DC02F775975F89L, 0x928125F8CD2F733DL,
		0x8BA92E4143ACC451L, 0x85230D5E4CF9A2E7L, 0x80F53442FAE81817L, 0xEC79CE4DFE225559L,
		0xE8C0DFC0BFBA8355L, 0xE17BC65AEC5B2B4FL, 0xC9C7ED90B7DC8245L, 0xBD5F6FC8E90C5D07L,
		0xBA64511D0D828555L, 0xA97757A8E386344DL, 0xA6CC70D55D35F399L, 0x9543D3B10C5615B5L,
		0x92EA54A31A94C409L, 0x8C160EA2D009104FL, 0x8378C84CCB4BA1BDL, 0x8166FCCF7036C5DBL,
		0xFC371C6CEDDC7205L, 0xF87C8BD3AE403793L, 0xEA1D56E2BDA00BFFL, 0xE6A747D3B0C88081L,
		0xE33E5241413D4A33L, 0xDFE244976AAB90CBL, 0xD619A6334F84AC69L, 0xCFD1025F2C00079DL,
		0xCCBE7B507B4957D5L, 0xC9B7950BABBC29B5L, 0xC6BC238E6BBBEEC7L, 0xC3CBFB7CF9900767L,
		0xC0E6F21FACFF9B93L, 0xB878EC7EE2DA97A9L, 0xB30EE46FAADDED93L, 0xA8B3AC7AC893846FL,
		0xA6352F1E5B3C1C03L, 0x9C983C1CCD7EBBEFL, 0x95C063A933738415L, 0x8F351956F9AE2877L,
		0x88F304B73771E64FL, 0x84EE0D14F7891817L, 0x82F6F2C80C8ACBFBL, 0xF8BF4767540B5989L,
		0xEADA280FBCD3AD3DL, 0xE432D1146EA76C1DL, 0xDA91DC9C6494C03BL, 0xCE5C47E7C0A49B07L,
		0xC88396110B59D367L, 0xC5A7374BA1B0CBF9L, 0xC00DAC74AD91C33BL, 0xBD50349A2C4B5513L,
		0xBA9CBED60294744FL, 0xB55347DE7F6D94E1L, 0xB2BCFF1DEBE7886DL, 0xB0302955736B386BL,
		0xADACA401E3552FB7L, 0xA3F9121B1DE2BB8FL, 0x9F53CE1B0A3FBF0DL, 0x9AD03BA1854A6351L,
		0x966D664F33C77D53L, 0x8E06440E7917299BL, 0x86174B57D7ADD2E5L, 0xF8D25F3BE7FB8C7BL,
		0xF54EF8CFAF339DF7L, 0xF1D84564732D7D13L, 0xEE6E171301EB70CFL, 0xE13F113D975738FFL,
		0xDAEE323AEC2EB61DL, 0xCED3319BF524A8EDL, 0xC62FEBD7C1810C09L, 0xC3638BF5C14A6369L,
		0xB5FA928110B9C405L, 0xB0E0492BC81941F7L, 0xA4BC94576D635F85L, 0xA01E0FAEE97AAAF3L,
		0x9DDB4B89334233FBL, 0x9BA0B36AEF8257ADL, 0x996E29CA31D27F19L, 0x9520CDEDF10F9ECFL,
		0x9305C2AE6EDA8AB9L, 0x8EE666043947E8FFL, 0x86FF9F2C3946E83FL, 0xFC8C1FB2CA6255C7L,
		0xF9242B1AADEBBE4FL, 0xF27760F60D177BB7L, 0xE8C9A67C604F3EFDL, 0xE28D067C84C30FE3L,
		0xDF7ED38F6BBA81FBL, 0xD981EF3DD834A6D9L, 0xD692F57E5AEAFA9DL, 0xD3AE1C984A2467EFL,
		0xCB3AFBA6E7EED305L, 0xC5C91548D1648307L, 0xC31E3368CDB2B5FBL, 0xC07C87483546A027L,
		0xBDE3F11A9312A9D7L, 0xB3DA06CC2F8BA769L, 0xA80BD634F6031C45L, 0xA5C7A2241326D801L,
		0xA38B414F2315052DL, 0x9F298DA497790377L, 0x9D0405DCABFD4D61L, 0x96BF807EFDE25957L,
		0x90BB077FA55AC933L, 0x8EC753957AF2A37DL, 0x85681ABBB2067B7BL, 0xFC7A9A2A032A8977L,
		0xEEDF4C40B6E4AD29L, 0xE52693623DA357C7L, 0xE1FFB775C150F88DL, 0xDBD323984B89CEFBL,
		0xD2E0DDA155272C7BL, 0xCA4BCD2E220F1B75L, 0xC7837FEBFDE104C1L, 0xBCC2ED0A433D4C8DL,
		0xB296B1D029F71665L, 0xA8F6CFA5BBDA566FL, 0xA6A3DFC64D652EFBL, 0xA2166FB018D2B9ADL,
		0x9B7DB46D1025A921L, 0x973E3DA8A40A7899L, 0x8D1FC997345375ABL, 0x87617777C96B7D5DL,
		0x8584C793B2E41889L, 0x83AEA62449FA1E85L, 0xF91F1DA477858FEDL, 0xF5C07F1263DFFDA1L,
		0xF26D8B7221C57113L, 0xE8B920F29261309BL, 0xD39011984C254301L, 0xCDE0D80E809155D9L,
	};

	private static final int COPY_THRESHOLD = 128;
	private static final int NIL_ARRAY_SIZE = 1024;
	private static final Object[] NIL_ARRAY = new Object[NIL_ARRAY_SIZE];

	/**
	 * Not instantiable.
	 */
	private Utilities () {
	}

	private static float defaultLoadFactor = 0.7f;

	/**
	 * Sets the load factor that will be used when none is specified during construction (for
	 * data structures that have a load factor, such as all sets and maps here). The load factor
	 * will be clamped so that it is greater than 0 (the lowest possible is
	 * {@link #FLOAT_ROUNDING_ERROR}, but it should never actually be that low) and less than or
	 * equal to 1. The initial value for the default load factor is 0.7.
	 * <br>
	 * If multiple libraries and/or your own code depend on jdkgdxds, then they may attempt to set
	 * the default load factor independently of each other, but this only has one setting at a time.
	 * The best solution for this is to specify the load factor you want when it matters, possibly
	 * to a variable set per-library or even per section of a library that needs some load factor.
	 * That means <b>not using the default load factor in this class</b>, and always using the
	 * constructors that specify a load factor. Libraries are generally discouraged from setting the
	 * default load factor; that decision should be left up to the application using the library.
	 *
	 * @param loadFactor a float that will be clamped between 0 (exclusive) and 1 (inclusive)
	 */
	public static void setDefaultLoadFactor (float loadFactor) {
		defaultLoadFactor = Math.min(Math.max(loadFactor, FLOAT_ROUNDING_ERROR), 1f);
	}

	/**
	 * Gets the default load factor, meant to be used when no load factor is specified during the
	 * construction of a data structure such as a map or set. The initial value for the default
	 * load factor is 0.7.
	 *
	 * @return the default load factor, always between 0 (exclusive) and 1 (inclusive)
	 */
	public static float getDefaultLoadFactor () {
		return defaultLoadFactor;
	}

	/**
	 * Used to establish the size of a hash table for {@link ObjectSet}, {@link ObjectObjectMap}, and related code.
	 * The table size will always be a power of two, and should be the next power of two that is at least equal
	 * to {@code capacity / loadFactor}.
	 *
	 * @param capacity   the amount of items the hash table should be able to hold
	 * @param loadFactor between 0.0 (exclusive) and 1.0 (inclusive); the fraction of how much of the table can be filled
	 * @return the size of a hash table that can handle the specified capacity with the given loadFactor
	 */
	public static int tableSize (int capacity, float loadFactor) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
		}
		int tableSize = 1 << -BitConversion.countLeadingZeros(Math.max(2, (int)Math.ceil(capacity / loadFactor)) - 1);
		if (tableSize > 1 << 30 || tableSize < 0) {
			throw new IllegalArgumentException("The required capacity is too large: " + capacity);
		}
		return tableSize;
	}

	/**
	 * Set all elements in {@code objects} to null.
	 * This method is faster than {@link Arrays#fill} for large arrays (> 128).
	 * <br>
	 * From Apache Fury's ObjectArray class.
	 */
	public static void clear(Object[] objects) {
		clear(objects, 0, objects.length);
	}

	/**
	 * Set all {@code size} elements in {@code objects}, starting at index {@code start}, to null.
	 * This method is faster than {@link Arrays#fill} for large arrays (> 128).
	 * <br>
	 * From Apache Fury's ObjectArray class.
	 */
	public static void clear(Object[] objects, int start, int size) {
		if (size < COPY_THRESHOLD) {
			Arrays.fill(objects, start, size, null);
		} else {
			if (size < NIL_ARRAY_SIZE) {
				System.arraycopy(NIL_ARRAY, 0, objects, start, size);
			} else {
				while (size > NIL_ARRAY_SIZE) {
					System.arraycopy(NIL_ARRAY, 0, objects, start, NIL_ARRAY_SIZE);
					size -= NIL_ARRAY_SIZE;
					start += NIL_ARRAY_SIZE;
				}
				System.arraycopy(NIL_ARRAY, 0, objects, start, size);
			}
		}
	}

	/**
	 * A placeholder Object that should never be reference-equivalent to any Object used as a key or value. This is only public
	 * so data structures can use it for comparisons; never put it into a data structure.
	 */
	public static final Object neverIdentical = new Object();

	/**
	 * A float that is meant to be used as the smallest reasonable tolerance for methods like {@link #isEqual(float, float, float)}.
	 */
	public static final float FLOAT_ROUNDING_ERROR = 0.000001f;

	/**
	 * Equivalent to libGDX's isEqual() method in MathUtils; this compares two floats for equality and allows just enough
	 * tolerance to ignore a rounding error. An example is {@code 0.3f - 0.2f == 0.1f} vs. {@code isEqual(0.3f - 0.2f, 0.1f)};
	 * the first is incorrectly false, while the second is correctly true.
	 *
	 * @param a the first float to compare
	 * @param b the second float to compare
	 * @return true if a and b are equal or extremely close to equal, or false otherwise.
	 */
	public static boolean isEqual (float a, float b) {
		return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
	}

	/**
	 * Equivalent to libGDX's isEqual() method in MathUtils; this compares two floats for equality and allows the given
	 * tolerance during comparison. An example is {@code 0.3f - 0.2f == 0.1f} vs. {@code isEqual(0.3f - 0.2f, 0.1f, 0.000001f)};
	 * the first is incorrectly false, while the second is correctly true.
	 *
	 * @param a         the first float to compare
	 * @param b         the second float to compare
	 * @param tolerance the maximum difference between a and b permitted for this to return true, inclusive
	 * @return true if a and b have a difference less than or equal to tolerance, or false otherwise.
	 */
	public static boolean isEqual (float a, float b, float tolerance) {
		return Math.abs(a - b) <= tolerance;
	}

	/**
	 * A simple equality comparison for {@link CharSequence} values such as {@link String}s or {@link StringBuilder}s
	 * that ignores case by upper-casing any cased letters. This works for all alphabets in Unicode except Georgian.
	 *
	 * @param a a non-null CharSequence, such as a String or StringBuilder
	 * @param b a non-null CharSequence, such as a String or StringBuilder
	 * @return whether the contents of {@code a} and {@code b} are equal ignoring case
	 */
	public static boolean equalsIgnoreCase (CharSequence a, CharSequence b) {
		if (a == b)
			return true;
		final int al = a.length();
		if (al != b.length())
			return false;
		for (int i = 0; i < al; i++) {
			char ac = a.charAt(i), bc = b.charAt(i);
			if (ac != bc && Character.toUpperCase(ac) != Character.toUpperCase(bc)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Like {@link String#compareToIgnoreCase(String)}, but works for all {@link CharSequence} values, such as
	 * {@link String}s or {@link StringBuilder}s; this ignores case by upper-casing any cased letters.
	 * This technique works for all alphabets in Unicode except Georgian.
	 *
	 * @param l a non-null CharSequence, such as a String or StringBuilder
	 * @param r a non-null CharSequence, such as a String or StringBuilder
	 * @return  ignoring case: 0 if the two {@code CharSequence}s are equal;
	 *          a negative integer if {@code l}
	 *          is lexicographically less than {@code r}; or a
	 *          positive integer if {@code l} is
	 *          lexicographically greater than {@code r}
	 */
	public static int compareIgnoreCase (CharSequence l, CharSequence r) {
		if (l == r)
			return 0;
		for (int i = 0, len = Math.min(l.length(), r.length()); i < len; i++) {
			char a = Character.toUpperCase(l.charAt(i));
			char b = Character.toUpperCase(r.charAt(i));
			if (a != b) {
				return a - b;
			}
		}

		return l.length() - r.length();
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Water hash, which is a variant on <a href="https://github.com/vnmakarov/mum-hash">mum-hash</a> and
	 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a>. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 * <br>
	 * This is very similar to the {@link Hasher#hash64(CharSequence)} method, and shares
	 * the same constants and mum() method with Hasher.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @return a long hashCode; quality should be similarly good across any bits
	 */
	public static long longHashCodeIgnoreCase (final CharSequence data) {
		return longHashCodeIgnoreCase(data, 9069147967908697017L);
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Water hash, which is a variant on <a href="https://github.com/vnmakarov/mum-hash">mum-hash</a> and
	 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a>. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 * <br>
	 * This is very similar to the {@link Hasher#hash64(CharSequence)} method, and shares
	 * the same constants and mum() method with Hasher.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @param seed any long; must be the same between calls if two equivalent values for {@code data} must be the same
	 * @return a long hashCode; quality should be similarly good across any bits
	 */
	public static long longHashCodeIgnoreCase (final CharSequence data, long seed) {
		final int len = data.length();
		for (int i = 3; i < len; i += 4) {
			seed = mum(
				mum(Character.toUpperCase(data.charAt(i - 3)) ^ b1, Character.toUpperCase(data.charAt(i - 2)) ^ b2) - seed,
				mum(Character.toUpperCase(data.charAt(i - 1)) ^ b3, Character.toUpperCase(data.charAt(i)) ^ b4));
		}

		switch (len & 3) {
		case 0:
			seed = mum(b1 - seed, b4 + seed);
			break;
		case 1:
			seed = mum(b5 - seed, b3 ^ Character.toUpperCase(data.charAt(len - 1)));
			break;
		case 2:
			seed = mum(Character.toUpperCase(data.charAt(len - 2)) - seed, b0 ^ Character.toUpperCase(data.charAt(len - 1)));
			break;
		case 3:
			seed = mum(Character.toUpperCase(data.charAt(len - 3)) - seed, b2 ^ Character.toUpperCase(data.charAt(len - 2))) + mum(b5 ^ seed, b4 ^ Character.toUpperCase(data.charAt(len - 1)));
			break;
		}
		seed = (seed ^ len) * (seed << 16 ^ b0);
		return (seed ^ (seed << 33 | seed >>> 31) ^ (seed << 19 | seed >>> 45));
	}

}
