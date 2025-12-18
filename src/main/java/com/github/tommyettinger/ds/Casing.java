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

/**
 * This mostly-internal class only exists to help case-insensitive comparisons and hashing.
 * It stores a massive char array in source code that contains transforms of every Java char
 * after calling {@link Character#toUpperCase(char)}. This data is only accessible via
 * {@link #caseUp(char)}. There is no and will be no caseDown() unless we find we need it
 * for case-insensitive code of some kind.
 */
public final class Casing {
	private static final char[] allToUpper = new char[65536];
	static {
		String k = "abcdefghijklmnopqrstuvwxyzµàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿāăąćĉċčďđēĕėęěĝğġģĥħĩīĭįıĳĵķĺļľŀłńņňŋōŏőœŕŗřśŝşšţťŧũūŭůűųŵŷźżžſƀƃƅƈƌƒƕƙƚƛƞơƣƥƨƭưƴƶƹƽƿǅǆǈǉǋǌǎǐǒǔǖǘǚǜǝǟǡǣǥǧǩǫǭǯǲǳǵǹǻǽǿȁȃȅȇȉȋȍȏȑȓȕȗșțȝȟȣȥȧȩȫȭȯȱȳȼȿɀɂɇɉɋɍɏɐɑɒɓɔɖɗəɛɜɠɡɣɤɥɦɨɩɪɫɬɯɱɲɵɽʀʂʃʇʈʉʊʋʌʒʝʞͅͱͳͷͻͼͽάέήίαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϕϖϗϙϛϝϟϡϣϥϧϩϫϭϯϰϱϲϳϵϸϻабвгдежзийклмнопрстуфхцчшщъыьэюяѐёђѓєѕіїјљњћќѝўџѡѣѥѧѩѫѭѯѱѳѵѷѹѻѽѿҁҋҍҏґғҕҗҙқҝҟҡңҥҧҩҫҭүұҳҵҷҹһҽҿӂӄӆӈӊӌӎӏӑӓӕӗәӛӝӟӡӣӥӧөӫӭӯӱӳӵӷӹӻӽӿԁԃԅԇԉԋԍԏԑԓԕԗԙԛԝԟԡԣԥԧԩԫԭԯաբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆაბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰჱჲჳჴჵჶჷჸჹჺჽჾჿᏸᏹᏺᏻᏼᏽᲀᲁᲂᲃᲄᲅᲆᲇᲈᲊᵹᵽᶎḁḃḅḇḉḋḍḏḑḓḕḗḙḛḝḟḡḣḥḧḩḫḭḯḱḳḵḷḹḻḽḿṁṃṅṇṉṋṍṏṑṓṕṗṙṛṝṟṡṣṥṧṩṫṭṯṱṳṵṷṹṻṽṿẁẃẅẇẉẋẍẏẑẓẕẛạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹỻỽỿἀἁἂἃἄἅἆἇἐἑἒἓἔἕἠἡἢἣἤἥἦἧἰἱἲἳἴἵἶἷὀὁὂὃὄὅὑὓὕὗὠὡὢὣὤὥὦὧὰάὲέὴήὶίὸόὺύὼώᾀᾁᾂᾃᾄᾅᾆᾇᾐᾑᾒᾓᾔᾕᾖᾗᾠᾡᾢᾣᾤᾥᾦᾧᾰᾱᾳιῃῐῑῠῡῥῳⅎⅰⅱⅲⅳⅴⅵⅶⅷⅸⅹⅺⅻⅼⅽⅾⅿↄⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩⰰⰱⰲⰳⰴⰵⰶⰷⰸⰹⰺⰻⰼⰽⰾⰿⱀⱁⱂⱃⱄⱅⱆⱇⱈⱉⱊⱋⱌⱍⱎⱏⱐⱑⱒⱓⱔⱕⱖⱗⱘⱙⱚⱛⱜⱝⱞⱟⱡⱥⱦⱨⱪⱬⱳⱶⲁⲃⲅⲇⲉⲋⲍⲏⲑⲓⲕⲗⲙⲛⲝⲟⲡⲣⲥⲧⲩⲫⲭⲯⲱⲳⲵⲷⲹⲻⲽⲿⳁⳃⳅⳇⳉⳋⳍⳏⳑⳓⳕⳗⳙⳛⳝⳟⳡⳣⳬⳮⳳⴀⴁⴂⴃⴄⴅⴆⴇⴈⴉⴊⴋⴌⴍⴎⴏⴐⴑⴒⴓⴔⴕⴖⴗⴘⴙⴚⴛⴜⴝⴞⴟⴠⴡⴢⴣⴤⴥⴧⴭꙁꙃꙅꙇꙉꙋꙍꙏꙑꙓꙕꙗꙙꙛꙝꙟꙡꙣꙥꙧꙩꙫꙭꚁꚃꚅꚇꚉꚋꚍꚏꚑꚓꚕꚗꚙꚛꜣꜥꜧꜩꜫꜭꜯꜳꜵꜷꜹꜻꜽꜿꝁꝃꝅꝇꝉꝋꝍꝏꝑꝓꝕꝗꝙꝛꝝꝟꝡꝣꝥꝧꝩꝫꝭꝯꝺꝼꝿꞁꞃꞅꞇꞌꞑꞓꞔꞗꞙꞛꞝꞟꞡꞣꞥꞧꞩꞵꞷꞹꞻꞽꞿꟁꟃꟈꟊꟍꟑꟗꟙꟛꟶꭓꭰꭱꭲꭳꭴꭵꭶꭷꭸꭹꭺꭻꭼꭽꭾꭿꮀꮁꮂꮃꮄꮅꮆꮇꮈꮉꮊꮋꮌꮍꮎꮏꮐꮑꮒꮓꮔꮕꮖꮗꮘꮙꮚꮛꮜꮝꮞꮟꮠꮡꮢꮣꮤꮥꮦꮧꮨꮩꮪꮫꮬꮭꮮꮯꮰꮱꮲꮳꮴꮵꮶꮷꮸꮹꮺꮻꮼꮽꮾꮿａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ",
			v = "                          ﴙ                              ﾇèĬ］ﾟ｝妿ｾ￈O헁헁헡헤헢ÒÎÍÍÊË媱Í媵Ï媙嫘媼ÑÓ媼혉媿Ó혃ÕÖ혙Ú媽Ú嫖ÚEÙÙGÛ嫫嫮ﾬｾｾｾ&%%%                          @??>9/6VP￹t`                                PPPPPPPPPPPPPPPP00000000000000000000000000000000000000ᡮᡭᡤᡢᡢᡣᡜᠥ瘾痼痈;￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸ﾶﾶﾪﾪﾪﾪﾜﾜﾀﾀﾐﾐﾂﾂ￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￸￷ᰥ￷￸￸￸￸￹￷000000000000000000000000000000000000000000000000⨫⨨ᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠᱠ￐Π韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐韐                          ";

		for (char i = 1; i != 0; i++) {
			allToUpper[i] = i;
		}
		int len = k.length();
		for (int i = 0; i < len; i++) {
			char key = k.charAt(i);
			allToUpper[key] = (char) (key - v.charAt(i));
		}

	}
	private Casing() {
	}

	/**
	 * Gets what would be the result of calling {@link Character#toUpperCase(char)} on {@code c}, but works identically
	 * on all platforms, and should be very fast. This should match the behavior of Java 25's Character class.
	 *
	 * @param c any char
	 * @return {@code c}, transformed to upper case if possible or left the same if not.
	 */
	public static char caseUp(char c) {
		return allToUpper[c];
	}
}
