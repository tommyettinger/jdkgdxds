/*
 * Copyright (c) 2025 See AUTHORS file.
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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.Base;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Casing2Generator {
	public static char[] load(CharSequence ku, CharSequence vu, CharSequence kd, CharSequence vd) {
		char[] allToUpper = new char[65536];
		char[] allToLower = new char[65536];
		for (char i = 1; i != 0; i++) {
			allToLower[i] = allToUpper[i] = i;
		}
		int lu = Math.min(ku.length(), vu.length());
		for (int i = 0; i < lu; i++) {
			char key = ku.charAt(i);
			allToUpper[key] = (char) (key - vu.charAt(i));
		}
		int ld = Math.min(kd.length(), vd.length());
		for (int i = 0; i < ld; i++) {
			char key = kd.charAt(i);
			allToLower[key] = (char) (key + vd.charAt(i));
		}

		return allToLower;
	}

	public static void main(String[] args) throws IOException {
		StringBuilder keys = new StringBuilder(2000);
		StringBuilder vals = new StringBuilder(2000);
		for (char i = '\u0001'; i != 0; i++) {
			char lower = Character.toLowerCase(i);
			if(i != lower){
				if(i == '\\' || i == '"')
					keys.append('\\');
				keys.append(i);
				char diff = (char) (lower - i);
				if(i == '\u212A' || i == '\u212B'){
					System.out.println("Problem char " + Base.BASE16.unsigned((short) i)
						+ " has toLowerCase " + Base.BASE16.unsigned((short) lower)
						+ " and diff " + Base.BASE16.unsigned((short) diff));
					System.out.println("Array will report " + Base.BASE16.unsigned((short) (i + diff)));
				}
				if(diff == '\\' || diff == '"')
					vals.append('\\');
				vals.append(diff);
			}
		}
//		for (char i = '\u0001'; i != 0; i++) {
//			char upper = Character.toUpperCase(i);
//			if(i != upper){
//				if(i == '\\' || i == '"')
//					keys.append('\\');
//				keys.append(i);
//				char diff = (char) (i - upper);
//				if(diff == '\\' || diff == '"')
//					vals.append('\\');
//				vals.append(diff);
//			}
//		}
		System.out.println("(\"" + keys + "\",");
		System.out.println(" \"" + vals + "\")");
		System.out.println("Done!");
		System.out.println("keys.length() = " + keys.length());
		System.out.println("vals.length() = " + vals.length());
		System.out.println("keys bytes length = " + keys.toString().getBytes(StandardCharsets.UTF_8).length);
		System.out.println("vals bytes length = " + vals.toString().getBytes(StandardCharsets.UTF_8).length);

		String ku = "abcdefghijklmnopqrstuvwxyzµàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿāăąćĉċčďđēĕėęěĝğġģĥħĩīĭįıĳĵķĺļľŀłńņňŋōŏőœŕŗřśŝşšţťŧũūŭůűųŵŷźżžſƀƃƅƈƌƒƕƙƚƛƞơƣƥƨƭưƴƶƹƽƿǅǆǈǉǋǌǎǐǒǔǖǘǚǜǝǟǡǣǥǧǩǫǭǯǲǳǵǹǻǽǿȁȃȅȇȉȋȍȏȑȓȕȗșțȝȟȣȥȧȩȫȭȯȱȳȼȿɀɂɇɉɋɍɏɐɑɒɓɔɖɗəɛɜɠɡɣɤɥɦɨɩɪɫɬɯɱɲɵɽʀʂʃʇʈʉʊʋʌʒʝʞͅͱͳͷͻͼͽάέήίαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϕϖϗϙϛϝϟϡϣϥϧϩϫϭϯϰϱϲϳϵϸϻабвгдежзийклмнопрстуфхцчшщъыьэюяѐёђѓєѕіїјљњћќѝўџѡѣѥѧѩѫѭѯѱѳѵѷѹѻѽѿҁҋҍҏґғҕҗҙқҝҟҡңҥҧҩҫҭүұҳҵҷҹһҽҿӂӄӆӈӊӌӎӏӑӓӕӗәӛӝӟӡӣӥӧөӫӭӯӱӳӵӷӹӻӽӿԁԃԅԇԉԋԍԏԑԓԕԗԙԛԝԟԡԣԥԧԩԫԭԯաբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆაბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰჱჲჳჴჵჶჷჸჹჺჽჾჿᏸᏹᏺᏻᏼᏽᲀᲁᲂᲃᲄᲅᲆᲇᲈᲊᵹᵽᶎḁḃḅḇḉḋḍḏḑḓḕḗḙḛḝḟḡḣḥḧḩḫḭḯḱḳḵḷḹḻḽḿṁṃṅṇṉṋṍṏṑṓṕṗṙṛṝṟṡṣṥṧṩṫṭṯṱṳṵṷṹṻṽṿẁẃẅẇẉẋẍẏẑẓẕẛạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹỻỽỿἀἁἂἃἄἅἆἇἐἑἒἓἔἕἠἡἢἣἤἥἦἧἰἱἲἳἴἵἶἷὀὁὂὃὄὅὑὓὕὗὠὡὢὣὤὥὦὧὰάὲέὴήὶίὸόὺύὼώᾀᾁᾂᾃᾄᾅᾆᾇᾐᾑᾒᾓᾔᾕᾖᾗᾠᾡᾢᾣᾤᾥᾦᾧᾰᾱᾳιῃῐῑῠῡῥῳⅎⅰⅱⅲⅳⅴⅵⅶⅷⅸⅹⅺⅻⅼⅽⅾⅿↄⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩⰰⰱⰲⰳⰴⰵⰶⰷⰸⰹⰺⰻⰼⰽⰾⰿⱀⱁⱂⱃⱄⱅⱆⱇⱈⱉⱊⱋⱌⱍⱎⱏⱐⱑⱒⱓⱔⱕⱖⱗⱘⱙⱚⱛⱜⱝⱞⱟⱡⱥⱦⱨⱪⱬⱳⱶⲁⲃⲅⲇⲉⲋⲍⲏⲑⲓⲕⲗⲙⲛⲝⲟⲡⲣⲥⲧⲩⲫⲭⲯⲱⲳⲵⲷⲹⲻⲽⲿⳁⳃⳅⳇⳉⳋⳍⳏⳑⳓⳕⳗⳙⳛⳝⳟⳡⳣⳬⳮⳳⴀⴁⴂⴃⴄⴅⴆⴇⴈⴉⴊⴋⴌⴍⴎⴏⴐⴑⴒⴓⴔⴕⴖⴗⴘⴙⴚⴛⴜⴝⴞⴟⴠⴡⴢⴣⴤⴥⴧⴭꙁꙃꙅꙇꙉꙋꙍꙏꙑꙓꙕꙗꙙꙛꙝꙟꙡꙣꙥꙧꙩꙫꙭꚁꚃꚅꚇꚉꚋꚍꚏꚑꚓꚕꚗꚙꚛꜣꜥꜧꜩꜫꜭꜯꜳꜵꜷꜹꜻꜽꜿꝁꝃꝅꝇꝉꝋꝍꝏꝑꝓꝕꝗꝙꝛꝝꝟꝡꝣꝥꝧꝩꝫꝭꝯꝺꝼꝿꞁꞃꞅꞇꞌꞑꞓꞔꞗꞙꞛꞝꞟꞡꞣꞥꞧꞩꞵꞷꞹꞻꞽꞿꟁꟃꟈꟊꟍꟑꟗꟙꟛꟶꭓꭰꭱꭲꭳꭴꭵꭶꭷꭸꭹꭺꭻꭼꭽꭾꭿꮀꮁꮂꮃꮄꮅꮆꮇꮈꮉꮊꮋꮌꮍꮎꮏꮐꮑꮒꮓꮔꮕꮖꮗꮘꮙꮚꮛꮜꮝꮞꮟꮠꮡꮢꮣꮤꮥꮦꮧꮨꮩꮪꮫꮬꮭꮮꮯꮰꮱꮲꮳꮴꮵꮶꮷꮸꮹꮺꮻꮼꮽꮾꮿａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ",
			vu = "                          ﴙ                              ﾇèĬ］ﾟ｝妿ｾ￈O헁헁헡헤헢ÒÎÍÍÊË媱Í媵Ï媙嫘媼ÑÓ媼혉媿Ó혃ÕÖ혙Ú媽Ú嫖ÚEÙÙGÛ嫫嫮ﾬｾｾｾ&%%%                          @??>9/6VP￹t`                                PPPPPPPPPPPPPPPP00000000000000000000000000000000000000ᡮᡭᡤᡢᡢᡣᡜᠥ瘾痼痈;￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸ﾶﾶﾪﾪﾪﾪﾜﾜﾀﾀﾐﾐﾂﾂ￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￷ᰥ￷￸￸￸￸￹￷000000000000000000000000000000000000000000000000⨫⨨ᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠ￐Π韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐                          ";

		String kd = "ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞĀĂĄĆĈĊČĎĐĒĔĖĘĚĜĞĠĢĤĦĨĪĬĮİĲĴĶĹĻĽĿŁŃŅŇŊŌŎŐŒŔŖŘŚŜŞŠŢŤŦŨŪŬŮŰŲŴŶŸŹŻŽƁƂƄƆƇƉƊƋƎƏƐƑƓƔƖƗƘƜƝƟƠƢƤƦƧƩƬƮƯƱƲƳƵƷƸƼǄǅǇǈǊǋǍǏǑǓǕǗǙǛǞǠǢǤǦǨǪǬǮǱǲǴǶǷǸǺǼǾȀȂȄȆȈȊȌȎȐȒȔȖȘȚȜȞȠȢȤȦȨȪȬȮȰȲȺȻȽȾɁɃɄɅɆɈɊɌɎͰͲͶͿΆΈΉΊΌΎΏΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫϏϘϚϜϞϠϢϤϦϨϪϬϮϴϷϹϺϽϾϿЀЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯѠѢѤѦѨѪѬѮѰѲѴѶѸѺѼѾҀҊҌҎҐҒҔҖҘҚҜҞҠҢҤҦҨҪҬҮҰҲҴҶҸҺҼҾӀӁӃӅӇӉӋӍӐӒӔӖӘӚӜӞӠӢӤӦӨӪӬӮӰӲӴӶӸӺӼӾԀԂԄԆԈԊԌԎԐԒԔԖԘԚԜԞԠԢԤԦԨԪԬԮԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՊՋՌՍՎՏՐՑՒՓՔՕՖႠႡႢႣႤႥႦႧႨႩႪႫႬႭႮႯႰႱႲႳႴႵႶႷႸႹႺႻႼႽႾႿჀჁჂჃჄჅჇჍᎠᎡᎢᎣᎤᎥᎦᎧᎨᎩᎪᎫᎬᎭᎮᎯᎰᎱᎲᎳᎴᎵᎶᎷᎸᎹᎺᎻᎼᎽᎾᎿᏀᏁᏂᏃᏄᏅᏆᏇᏈᏉᏊᏋᏌᏍᏎᏏᏐᏑᏒᏓᏔᏕᏖᏗᏘᏙᏚᏛᏜᏝᏞᏟᏠᏡᏢᏣᏤᏥᏦᏧᏨᏩᏪᏫᏬᏭᏮᏯᏰᏱᏲᏳᏴᏵᲉᲐᲑᲒᲓᲔᲕᲖᲗᲘᲙᲚᲛᲜᲝᲞᲟᲠᲡᲢᲣᲤᲥᲦᲧᲨᲩᲪᲫᲬᲭᲮᲯᲰᲱᲲᲳᲴᲵᲶᲷᲸᲹᲺᲽᲾᲿḀḂḄḆḈḊḌḎḐḒḔḖḘḚḜḞḠḢḤḦḨḪḬḮḰḲḴḶḸḺḼḾṀṂṄṆṈṊṌṎṐṒṔṖṘṚṜṞṠṢṤṦṨṪṬṮṰṲṴṶṸṺṼṾẀẂẄẆẈẊẌẎẐẒẔẞẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼẾỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸỺỼỾἈἉἊἋἌἍἎἏἘἙἚἛἜἝἨἩἪἫἬἭἮἯἸἹἺἻἼἽἾἿὈὉὊὋὌὍὙὛὝὟὨὩὪὫὬὭὮὯᾈᾉᾊᾋᾌᾍᾎᾏᾘᾙᾚᾛᾜᾝᾞᾟᾨᾩᾪᾫᾬᾭᾮᾯᾸᾹᾺΆᾼῈΈῊΉῌῘῙῚΊῨῩῪΎῬῸΌῺΏῼΩKÅℲⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩⅪⅫⅬⅭⅮⅯↃⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏⰀⰁⰂⰃⰄⰅⰆⰇⰈⰉⰊⰋⰌⰍⰎⰏⰐⰑⰒⰓⰔⰕⰖⰗⰘⰙⰚⰛⰜⰝⰞⰟⰠⰡⰢⰣⰤⰥⰦⰧⰨⰩⰪⰫⰬⰭⰮⰯⱠⱢⱣⱤⱧⱩⱫⱭⱮⱯⱰⱲⱵⱾⱿⲀⲂⲄⲆⲈⲊⲌⲎⲐⲒⲔⲖⲘⲚⲜⲞⲠⲢⲤⲦⲨⲪⲬⲮⲰⲲⲴⲶⲸⲺⲼⲾⳀⳂⳄⳆⳈⳊⳌⳎⳐⳒⳔⳖⳘⳚⳜⳞⳠⳢⳫⳭⳲꙀꙂꙄꙆꙈꙊꙌꙎꙐꙒꙔꙖꙘꙚꙜꙞꙠꙢꙤꙦꙨꙪꙬꚀꚂꚄꚆꚈꚊꚌꚎꚐꚒꚔꚖꚘꚚꜢꜤꜦꜨꜪꜬꜮꜲꜴꜶꜸꜺꜼꜾꝀꝂꝄꝆꝈꝊꝌꝎꝐꝒꝔꝖꝘꝚꝜꝞꝠꝢꝤꝦꝨꝪꝬꝮꝹꝻꝽꝾꞀꞂꞄꞆꞋꞍꞐꞒꞖꞘꞚꞜꞞꞠꞢꞤꞦꞨꞪꞫꞬꞭꞮꞰꞱꞲꞳꞴꞶꞸꞺꞼꞾꟀꟂꟄꟅꟆꟇꟉꟋꟌꟐꟖꟘꟚꟜꟵＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ",
			vd = "                                                        ＹﾇÒÎÍÍOÊËÍÏÓÑÓÕÖÚÚÚÙÙÛﾟ￈ｾ⨫｝⨨］EGt&%%%@??                          ￄ￹ｾｾｾPPPPPPPPPPPPPPPP                                00000000000000000000000000000000000000ᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠ韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸ﾶﾶ￷ﾪﾪﾪﾪ￷￸￸ﾜﾜ￸￸ﾐﾐ￹ﾀﾀﾂﾂ￷??000000000000000000000000000000000000000000000000혉혙헤혃헡헢헁헁痼嫘媼媱媵媿媼嫮嫖嫫Π￐媽痈媙妿                          ";

		char[] caseDown = load
			(ku, vu, keys, vals);

		int casingMismatch = 0, characterMismatch = 0;
		for (char i = '\u0001'; i != 0; i++) {
//			if(caseUp[i] != Casing.caseUp(i)) {
////				System.out.println("Mismatch at index " + (int) i + ", array reported '" + (int) caseUp[i] + "', Casing reported '" + (int) Casing.caseUp(i) + "'...");
//				casingMismatch++;
//			}
			if(caseDown[i] != Character.toLowerCase(i)){
				System.out.println("Mismatch at index " + Base.BASE16.unsigned((short) i) + ", array reported '" + Base.BASE16.unsigned((short) caseDown[i]) + "', Character reported '" + Base.BASE16.unsigned((short) Character.toLowerCase(i)) + "'...");
				characterMismatch++;
			}
		}
		// Shows 10 mismatches with Character on Java 17, but none on Java 25. Never shows mismatches with Casing.
		System.out.println("There were " + casingMismatch + " mismatches with Casing and " + characterMismatch + " mismatches with Character.");

		//TODO: uncomment when we want to write a new Casing allToUpper generator.
		// This includes whenever there is a new JDK version with Unicode changes.
		Files.write(Paths.get("caseDown.txt"), ("String kd = \"" + keys + "\",\nvd = \"" + vals + "\";").getBytes(StandardCharsets.UTF_8));
	}
}
