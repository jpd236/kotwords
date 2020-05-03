(function (root, factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', 'kotlin', 'jszip'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('kotlin'), require('jszip'));
  else {
    if (typeof kotlin === 'undefined') {
      throw new Error("Error loading module 'kotwords'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'kotwords'.");
    }if (typeof JSZip === 'undefined') {
      throw new Error("Error loading module 'kotwords'. Its dependency 'jszip' was not found. Please, check whether 'jszip' is loaded prior to 'kotwords'.");
    }root.kotwords = factory(typeof kotwords === 'undefined' ? {} : kotwords, kotlin, JSZip);
  }
}(this, function (_, Kotlin, $module$jszip) {
  'use strict';
  var Enum = Kotlin.kotlin.Enum;
  var Kind_CLASS = Kotlin.Kind.CLASS;
  var throwISE = Kotlin.throwISE;
  var appendText = Kotlin.kotlin.dom.appendText_46n0ku$;
  var isBlank = Kotlin.kotlin.text.isBlank_gw00vp$;
  var Regex_init = Kotlin.kotlin.text.Regex_init_61zpoe$;
  var unboxChar = Kotlin.unboxChar;
  var to = Kotlin.kotlin.to_ujzrz7$;
  var listOf = Kotlin.kotlin.collections.listOf_i5x0yv$;
  var until = Kotlin.kotlin.ranges.until_dqglrj$;
  var IllegalStateException_init = Kotlin.kotlin.IllegalStateException_init;
  var toInt = Kotlin.kotlin.text.toInt_pdl1vz$;
  var replace = Kotlin.kotlin.text.replace_680rmw$;
  var split = Kotlin.kotlin.text.split_ip8yn$;
  var Kind_OBJECT = Kotlin.Kind.OBJECT;
  var IllegalArgumentException_init = Kotlin.kotlin.IllegalArgumentException_init_pdl1vj$;
  var ArrayList_init = Kotlin.kotlin.collections.ArrayList_init_287e2$;
  var iterator = Kotlin.kotlin.text.iterator_gw00vp$;
  var toBoxedChar = Kotlin.toBoxedChar;
  var LinkedHashMap_init = Kotlin.kotlin.collections.LinkedHashMap_init_q3lmfv$;
  var checkIndexOverflow = Kotlin.kotlin.collections.checkIndexOverflow_za3lpa$;
  ZipOutputType.prototype = Object.create(Enum.prototype);
  ZipOutputType.prototype.constructor = ZipOutputType;
  ZipOutputCompression.prototype = Object.create(Enum.prototype);
  ZipOutputCompression.prototype.constructor = ZipOutputCompression;
  function ZipOutputType(name, ordinal, jsValue) {
    Enum.call(this);
    this.jsValue = jsValue;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ZipOutputType_initFields() {
    ZipOutputType_initFields = function () {
    };
    ZipOutputType$BASE64_instance = new ZipOutputType('BASE64', 0, 'base64');
  }
  var ZipOutputType$BASE64_instance;
  function ZipOutputType$BASE64_getInstance() {
    ZipOutputType_initFields();
    return ZipOutputType$BASE64_instance;
  }
  ZipOutputType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ZipOutputType',
    interfaces: [Enum]
  };
  function ZipOutputType$values() {
    return [ZipOutputType$BASE64_getInstance()];
  }
  ZipOutputType.values = ZipOutputType$values;
  function ZipOutputType$valueOf(name) {
    switch (name) {
      case 'BASE64':
        return ZipOutputType$BASE64_getInstance();
      default:throwISE('No enum constant com.jeffpdavidson.kotwords.jslib.ZipOutputType.' + name);
    }
  }
  ZipOutputType.valueOf_61zpoe$ = ZipOutputType$valueOf;
  function ZipOutputCompression(name, ordinal, jsValue) {
    Enum.call(this);
    this.jsValue = jsValue;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ZipOutputCompression_initFields() {
    ZipOutputCompression_initFields = function () {
    };
    ZipOutputCompression$DEFLATE_instance = new ZipOutputCompression('DEFLATE', 0, 'deflate');
  }
  var ZipOutputCompression$DEFLATE_instance;
  function ZipOutputCompression$DEFLATE_getInstance() {
    ZipOutputCompression_initFields();
    return ZipOutputCompression$DEFLATE_instance;
  }
  ZipOutputCompression.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ZipOutputCompression',
    interfaces: [Enum]
  };
  function ZipOutputCompression$values() {
    return [ZipOutputCompression$DEFLATE_getInstance()];
  }
  ZipOutputCompression.values = ZipOutputCompression$values;
  function ZipOutputCompression$valueOf(name) {
    switch (name) {
      case 'DEFLATE':
        return ZipOutputCompression$DEFLATE_getInstance();
      default:throwISE('No enum constant com.jeffpdavidson.kotwords.jslib.ZipOutputCompression.' + name);
    }
  }
  ZipOutputCompression.valueOf_61zpoe$ = ZipOutputCompression$valueOf;
  function newGenerateAsyncOptions(type, compression) {
    if (type === void 0)
      type = ZipOutputType$BASE64_getInstance();
    if (compression === void 0)
      compression = ZipOutputCompression$DEFLATE_getInstance();
    var options = {};
    options.type = type.jsValue;
    options.compression = compression.jsValue;
    return options;
  }
  function CrosswordSolverSettings(cursorColor, selectedCellsColor, completionMessage) {
    this.cursorColor = cursorColor;
    this.selectedCellsColor = selectedCellsColor;
    this.completionMessage = completionMessage;
  }
  CrosswordSolverSettings.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CrosswordSolverSettings',
    interfaces: []
  };
  CrosswordSolverSettings.prototype.component1 = function () {
    return this.cursorColor;
  };
  CrosswordSolverSettings.prototype.component2 = function () {
    return this.selectedCellsColor;
  };
  CrosswordSolverSettings.prototype.component3 = function () {
    return this.completionMessage;
  };
  CrosswordSolverSettings.prototype.copy_6hosri$ = function (cursorColor, selectedCellsColor, completionMessage) {
    return new CrosswordSolverSettings(cursorColor === void 0 ? this.cursorColor : cursorColor, selectedCellsColor === void 0 ? this.selectedCellsColor : selectedCellsColor, completionMessage === void 0 ? this.completionMessage : completionMessage);
  };
  CrosswordSolverSettings.prototype.toString = function () {
    return 'CrosswordSolverSettings(cursorColor=' + Kotlin.toString(this.cursorColor) + (', selectedCellsColor=' + Kotlin.toString(this.selectedCellsColor)) + (', completionMessage=' + Kotlin.toString(this.completionMessage)) + ')';
  };
  CrosswordSolverSettings.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.cursorColor) | 0;
    result = result * 31 + Kotlin.hashCode(this.selectedCellsColor) | 0;
    result = result * 31 + Kotlin.hashCode(this.completionMessage) | 0;
    return result;
  };
  CrosswordSolverSettings.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.cursorColor, other.cursorColor) && Kotlin.equals(this.selectedCellsColor, other.selectedCellsColor) && Kotlin.equals(this.completionMessage, other.completionMessage)))));
  };
  function Cell(x, y, solution, backgroundColor, number) {
    this.x = x;
    this.y = y;
    this.solution = solution;
    this.backgroundColor = backgroundColor;
    this.number = number;
  }
  Cell.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Cell',
    interfaces: []
  };
  Cell.prototype.component1 = function () {
    return this.x;
  };
  Cell.prototype.component2 = function () {
    return this.y;
  };
  Cell.prototype.component3 = function () {
    return this.solution;
  };
  Cell.prototype.component4 = function () {
    return this.backgroundColor;
  };
  Cell.prototype.component5 = function () {
    return this.number;
  };
  Cell.prototype.copy_8y8fkt$ = function (x, y, solution, backgroundColor, number) {
    return new Cell(x === void 0 ? this.x : x, y === void 0 ? this.y : y, solution === void 0 ? this.solution : solution, backgroundColor === void 0 ? this.backgroundColor : backgroundColor, number === void 0 ? this.number : number);
  };
  Cell.prototype.toString = function () {
    return 'Cell(x=' + Kotlin.toString(this.x) + (', y=' + Kotlin.toString(this.y)) + (', solution=' + Kotlin.toString(this.solution)) + (', backgroundColor=' + Kotlin.toString(this.backgroundColor)) + (', number=' + Kotlin.toString(this.number)) + ')';
  };
  Cell.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.x) | 0;
    result = result * 31 + Kotlin.hashCode(this.y) | 0;
    result = result * 31 + Kotlin.hashCode(this.solution) | 0;
    result = result * 31 + Kotlin.hashCode(this.backgroundColor) | 0;
    result = result * 31 + Kotlin.hashCode(this.number) | 0;
    return result;
  };
  Cell.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.x, other.x) && Kotlin.equals(this.y, other.y) && Kotlin.equals(this.solution, other.solution) && Kotlin.equals(this.backgroundColor, other.backgroundColor) && Kotlin.equals(this.number, other.number)))));
  };
  function Word(id, cells) {
    this.id = id;
    this.cells = cells;
  }
  Word.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Word',
    interfaces: []
  };
  Word.prototype.component1 = function () {
    return this.id;
  };
  Word.prototype.component2 = function () {
    return this.cells;
  };
  Word.prototype.copy_i3og25$ = function (id, cells) {
    return new Word(id === void 0 ? this.id : id, cells === void 0 ? this.cells : cells);
  };
  Word.prototype.toString = function () {
    return 'Word(id=' + Kotlin.toString(this.id) + (', cells=' + Kotlin.toString(this.cells)) + ')';
  };
  Word.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.id) | 0;
    result = result * 31 + Kotlin.hashCode(this.cells) | 0;
    return result;
  };
  Word.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.id, other.id) && Kotlin.equals(this.cells, other.cells)))));
  };
  function Clue(word, number, text) {
    this.word = word;
    this.number = number;
    this.text = text;
  }
  Clue.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Clue',
    interfaces: []
  };
  Clue.prototype.component1 = function () {
    return this.word;
  };
  Clue.prototype.component2 = function () {
    return this.number;
  };
  Clue.prototype.component3 = function () {
    return this.text;
  };
  Clue.prototype.copy_lsb1ts$ = function (word, number, text) {
    return new Clue(word === void 0 ? this.word : word, number === void 0 ? this.number : number, text === void 0 ? this.text : text);
  };
  Clue.prototype.toString = function () {
    return 'Clue(word=' + Kotlin.toString(this.word) + (', number=' + Kotlin.toString(this.number)) + (', text=' + Kotlin.toString(this.text)) + ')';
  };
  Clue.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.word) | 0;
    result = result * 31 + Kotlin.hashCode(this.number) | 0;
    result = result * 31 + Kotlin.hashCode(this.text) | 0;
    return result;
  };
  Clue.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.word, other.word) && Kotlin.equals(this.number, other.number) && Kotlin.equals(this.text, other.text)))));
  };
  function ClueList(title, clues) {
    this.title = title;
    this.clues = clues;
  }
  ClueList.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ClueList',
    interfaces: []
  };
  ClueList.prototype.component1 = function () {
    return this.title;
  };
  ClueList.prototype.component2 = function () {
    return this.clues;
  };
  ClueList.prototype.copy_puryjc$ = function (title, clues) {
    return new ClueList(title === void 0 ? this.title : title, clues === void 0 ? this.clues : clues);
  };
  ClueList.prototype.toString = function () {
    return 'ClueList(title=' + Kotlin.toString(this.title) + (', clues=' + Kotlin.toString(this.clues)) + ')';
  };
  ClueList.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.title) | 0;
    result = result * 31 + Kotlin.hashCode(this.clues) | 0;
    return result;
  };
  ClueList.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.title, other.title) && Kotlin.equals(this.clues, other.clues)))));
  };
  function Jpz(title, creator, copyright, description, grid, clues, crosswordSolverSettings) {
    this.title = title;
    this.creator = creator;
    this.copyright = copyright;
    this.description = description;
    this.grid = grid;
    this.clues = clues;
    this.crosswordSolverSettings = crosswordSolverSettings;
  }
  Jpz.prototype.asXmlString = function () {
    var doc = document.implementation.createDocument('', '', null);
    doc.appendChild(doc.createProcessingInstruction('xml', 'version=1.0'));
    var root = doc.createElement('crossword-compiler-applet');
    root.setAttribute('xmlns', 'http://crossword.info/xml/crossword-compiler');
    var appletSettings = doc.createElement('applet-settings');
    appletSettings.setAttribute('cursor-color', this.crosswordSolverSettings.cursorColor);
    appletSettings.setAttribute('selected-cells-color', this.crosswordSolverSettings.selectedCellsColor);
    var completion = doc.createElement('completion');
    completion.setAttribute('friendly-submit', 'false');
    completion.setAttribute('only-if-correct', 'true');
    appendText(completion, this.crosswordSolverSettings.completionMessage);
    appletSettings.appendChild(completion);
    var actions = doc.createElement('actions');
    actions.setAttribute('graphical-buttons', 'false');
    actions.setAttribute('wide-buttons', 'false');
    actions.setAttribute('buttons-layout', 'left');
    var revealWord = doc.createElement('reveal-word');
    revealWord.setAttribute('label', 'Reveal Word');
    actions.appendChild(revealWord);
    var revealLetter = doc.createElement('reveal-letter');
    revealLetter.setAttribute('label', 'Reveal Letter');
    actions.appendChild(revealLetter);
    var check = doc.createElement('check');
    check.setAttribute('label', 'Check');
    actions.appendChild(check);
    var solution = doc.createElement('solution');
    solution.setAttribute('label', 'Solution');
    actions.appendChild(solution);
    var pencil = doc.createElement('pencil');
    pencil.setAttribute('label', 'Pencil');
    actions.appendChild(pencil);
    appletSettings.appendChild(actions);
    root.appendChild(appletSettings);
    var rectangularPuzzle = doc.createElement('rectangular-puzzle');
    rectangularPuzzle.setAttribute('xmlns', 'http://crossword.info/xml/rectangular-puzzle');
    rectangularPuzzle.setAttribute('alphabet', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ');
    var metadata = doc.createElement('metadata');
    if (!isBlank(this.title)) {
      metadata.appendChild(appendText(doc.createElement('title'), this.title));
    }if (!isBlank(this.creator)) {
      metadata.appendChild(appendText(doc.createElement('creator'), this.creator));
    }if (!isBlank(this.copyright)) {
      metadata.appendChild(appendText(doc.createElement('copyright'), this.copyright));
    }if (!isBlank(this.description)) {
      metadata.appendChild(appendText(doc.createElement('description'), this.description));
    }rectangularPuzzle.appendChild(metadata);
    var crossword = doc.createElement('crossword');
    var gridElem = doc.createElement('grid');
    gridElem.setAttribute('width', this.grid.size.toString());
    gridElem.setAttribute('height', this.grid.get_za3lpa$(0).size.toString());
    var gridLook = doc.createElement('grid-look');
    gridLook.setAttribute('numbering-scheme', 'normal');
    gridElem.appendChild(gridLook);
    var tmp$;
    tmp$ = this.grid.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      tmp$_0 = element.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var cellElem = doc.createElement('cell');
        cellElem.setAttribute('x', element_0.x.toString());
        cellElem.setAttribute('y', element_0.y.toString());
        cellElem.setAttribute('solution', element_0.solution);
        cellElem.setAttribute('background-color', element_0.backgroundColor);
        if (element_0.number != null) {
          cellElem.setAttribute('number', element_0.number);
        }gridElem.appendChild(cellElem);
      }
    }
    crossword.appendChild(gridElem);
    var tmp$_1;
    tmp$_1 = this.clues.iterator();
    while (tmp$_1.hasNext()) {
      var element_1 = tmp$_1.next();
      var tmp$_2;
      tmp$_2 = element_1.clues.iterator();
      while (tmp$_2.hasNext()) {
        var element_2 = tmp$_2.next();
        var wordElem = doc.createElement('word');
        wordElem.setAttribute('id', element_2.word.id.toString());
        var tmp$_3;
        tmp$_3 = element_2.word.cells.iterator();
        while (tmp$_3.hasNext()) {
          var element_3 = tmp$_3.next();
          var cellsElem = doc.createElement('cells');
          cellsElem.setAttribute('x', element_3.x.toString());
          cellsElem.setAttribute('y', element_3.y.toString());
          wordElem.appendChild(cellsElem);
        }
        crossword.appendChild(wordElem);
      }
    }
    var tmp$_4;
    tmp$_4 = this.clues.iterator();
    while (tmp$_4.hasNext()) {
      var element_4 = tmp$_4.next();
      var cluesElem = doc.createElement('clues');
      cluesElem.setAttribute('ordering', 'normal');
      var titleElem = doc.createElement('title');
      titleElem.appendChild(appendText(doc.createElement('b'), element_4.title));
      cluesElem.appendChild(titleElem);
      var tmp$_5;
      tmp$_5 = element_4.clues.iterator();
      while (tmp$_5.hasNext()) {
        var element_5 = tmp$_5.next();
        var clueElem = doc.createElement('clue');
        clueElem.setAttribute('word', element_5.word.id.toString());
        clueElem.setAttribute('number', element_5.number);
        appendText(clueElem, element_5.text);
        cluesElem.appendChild(clueElem);
      }
      crossword.appendChild(cluesElem);
    }
    rectangularPuzzle.appendChild(crossword);
    root.appendChild(rectangularPuzzle);
    doc.appendChild(root);
    return (new XMLSerializer()).serializeToString(doc);
  };
  Jpz.prototype.asBase64JpzZip = function () {
    var zip = new $module$jszip();
    var tmp$ = this.title;
    zip.file(Regex_init('[^A-Za-z0-9]').replace_x2uqeu$(tmp$, '') + '.xml', this.asXmlString());
    var options = newGenerateAsyncOptions(ZipOutputType$BASE64_getInstance());
    return zip.generateAsync(options);
  };
  Jpz.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Jpz',
    interfaces: []
  };
  Jpz.prototype.component1 = function () {
    return this.title;
  };
  Jpz.prototype.component2 = function () {
    return this.creator;
  };
  Jpz.prototype.component3 = function () {
    return this.copyright;
  };
  Jpz.prototype.component4 = function () {
    return this.description;
  };
  Jpz.prototype.component5 = function () {
    return this.grid;
  };
  Jpz.prototype.component6 = function () {
    return this.clues;
  };
  Jpz.prototype.component7 = function () {
    return this.crosswordSolverSettings;
  };
  Jpz.prototype.copy_l42x0i$ = function (title, creator, copyright, description, grid, clues, crosswordSolverSettings) {
    return new Jpz(title === void 0 ? this.title : title, creator === void 0 ? this.creator : creator, copyright === void 0 ? this.copyright : copyright, description === void 0 ? this.description : description, grid === void 0 ? this.grid : grid, clues === void 0 ? this.clues : clues, crosswordSolverSettings === void 0 ? this.crosswordSolverSettings : crosswordSolverSettings);
  };
  Jpz.prototype.toString = function () {
    return 'Jpz(title=' + Kotlin.toString(this.title) + (', creator=' + Kotlin.toString(this.creator)) + (', copyright=' + Kotlin.toString(this.copyright)) + (', description=' + Kotlin.toString(this.description)) + (', grid=' + Kotlin.toString(this.grid)) + (', clues=' + Kotlin.toString(this.clues)) + (', crosswordSolverSettings=' + Kotlin.toString(this.crosswordSolverSettings)) + ')';
  };
  Jpz.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.title) | 0;
    result = result * 31 + Kotlin.hashCode(this.creator) | 0;
    result = result * 31 + Kotlin.hashCode(this.copyright) | 0;
    result = result * 31 + Kotlin.hashCode(this.description) | 0;
    result = result * 31 + Kotlin.hashCode(this.grid) | 0;
    result = result * 31 + Kotlin.hashCode(this.clues) | 0;
    result = result * 31 + Kotlin.hashCode(this.crosswordSolverSettings) | 0;
    return result;
  };
  Jpz.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.title, other.title) && Kotlin.equals(this.creator, other.creator) && Kotlin.equals(this.copyright, other.copyright) && Kotlin.equals(this.description, other.description) && Kotlin.equals(this.grid, other.grid) && Kotlin.equals(this.clues, other.clues) && Kotlin.equals(this.crosswordSolverSettings, other.crosswordSolverSettings)))));
  };
  function TwistsAndTurns(title, creator, copyright, description, width, height, twistBoxSize, turnsAnswers, turnsClues, twistsClues, lightTwistsColor, darkTwistsColor, crosswordSolverSettings) {
    TwistsAndTurns$Companion_getInstance();
    this.title = title;
    this.creator = creator;
    this.copyright = copyright;
    this.description = description;
    this.width = width;
    this.height = height;
    this.twistBoxSize = twistBoxSize;
    this.turnsAnswers = turnsAnswers;
    this.turnsClues = turnsClues;
    this.twistsClues = twistsClues;
    this.lightTwistsColor = lightTwistsColor;
    this.darkTwistsColor = darkTwistsColor;
    this.crosswordSolverSettings = crosswordSolverSettings;
    if (!(this.width % this.twistBoxSize === 0 && this.height % this.twistBoxSize === 0)) {
      var message = 'Width ' + this.width + ' and height ' + this.height + ' must evenly divide twist box size ' + this.twistBoxSize;
      throw IllegalArgumentException_init(message.toString());
    }var neededTwistClues = Kotlin.imul(this.width / this.twistBoxSize | 0, this.height / this.twistBoxSize | 0);
    if (!(this.twistsClues.size === neededTwistClues)) {
      var message_0 = 'Grid size requires ' + neededTwistClues + ' twist clues but have ' + this.twistsClues.size;
      throw IllegalArgumentException_init(message_0.toString());
    }if (!(this.turnsAnswers.size === this.turnsClues.size)) {
      var message_1 = 'Have ' + this.turnsAnswers.size + ' turns answers but ' + this.turnsClues.size + ' turns clues';
      throw IllegalArgumentException_init(message_1.toString());
    }var tmp$;
    var accumulator = 0;
    tmp$ = this.turnsAnswers.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      accumulator = accumulator + element.length | 0;
    }
    var cellCount = accumulator;
    if (!(cellCount === Kotlin.imul(this.width, this.height))) {
      var message_2 = 'Have ' + cellCount + ' letters in turns answers but need ' + Kotlin.imul(this.width, this.height);
      throw IllegalArgumentException_init(message_2.toString());
    }}
  TwistsAndTurns.prototype.asJpz = function () {
    var x = {v: 1};
    var y = {v: 1};
    var turnsCluesList = ArrayList_init();
    var cellMap = LinkedHashMap_init();
    var tmp$, tmp$_0;
    var index = 0;
    tmp$ = this.turnsAnswers.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var answerIndex = checkIndexOverflow((tmp$_0 = index, index = tmp$_0 + 1 | 0, tmp$_0));
      var word = ArrayList_init();
      var clueNumber = answerIndex + 1 | 0;
      var tmp$_1, tmp$_0_0;
      var index_0 = 0;
      tmp$_1 = iterator(item);
      while (tmp$_1.hasNext()) {
        var item_0 = unboxChar(tmp$_1.next());
        var chIndex = (tmp$_0_0 = index_0, index_0 = tmp$_0_0 + 1 | 0, tmp$_0_0);
        var ch = toBoxedChar(item_0);
        var tmp$_2, tmp$_3;
        if (chIndex === 0) {
          tmp$_2 = clueNumber.toString();
        } else {
          tmp$_2 = null;
        }
        var number = tmp$_2;
        if (((x.v - 1 | 0) / this.twistBoxSize | 0) % 2 === ((y.v - 1 | 0) / this.twistBoxSize | 0) % 2) {
          tmp$_3 = '#FFFFFF';
        } else {
          tmp$_3 = '#999999';
        }
        var backgroundColor = tmp$_3;
        var cell = new Cell(x.v, y.v, String.fromCharCode(unboxChar(ch)), backgroundColor, number);
        var key = to(x.v, y.v);
        cellMap.put_xwzc9p$(key, cell);
        word.add_11rb$(cell);
        if ((y.v - 1 | 0) % 2 === 0) {
          if (x.v === this.width) {
            y.v = y.v + 1 | 0;
          } else {
            x.v = x.v + 1 | 0;
          }
        } else {
          if (x.v === 1) {
            y.v = y.v + 1 | 0;
          } else {
            x.v = x.v - 1 | 0;
          }
        }
      }
      turnsCluesList.add_11rb$(new Clue(new Word(clueNumber, word), clueNumber.toString(), this.turnsClues.get_za3lpa$(answerIndex)));
    }
    var grid = this.generateGrid_0(cellMap);
    return new Jpz(this.title, this.creator, this.copyright, this.description, grid, listOf([new ClueList('Turns', turnsCluesList), new ClueList('Twists', this.generateTwistsCluesList_0(grid))]), this.crosswordSolverSettings);
  };
  TwistsAndTurns.prototype.generateGrid_0 = function (cellMap) {
    var grid = ArrayList_init();
    var tmp$;
    tmp$ = until(0, this.height).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var row = ArrayList_init();
      var tmp$_0;
      tmp$_0 = until(0, this.width).iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var tmp$_1;
        tmp$_1 = cellMap.get_11rb$(to(element_0 + 1 | 0, element + 1 | 0));
        if (tmp$_1 == null) {
          throw IllegalStateException_init();
        }row.add_11rb$(tmp$_1);
      }
      grid.add_11rb$(row);
    }
    return grid;
  };
  TwistsAndTurns.prototype.generateTwistsCluesList_0 = function (grid) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2, tmp$_3, tmp$_4;
    var twistsCluesList = ArrayList_init();
    var twistNumber = 0;
    tmp$ = this.height / this.twistBoxSize | 0;
    for (var j = 0; j < tmp$; j++) {
      tmp$_0 = this.width / this.twistBoxSize | 0;
      for (var i = 0; i < tmp$_0; i++) {
        var cells = ArrayList_init();
        tmp$_1 = Kotlin.imul(j, this.twistBoxSize) + 1 | 0;
        tmp$_2 = Kotlin.imul(j + 1 | 0, this.twistBoxSize);
        for (var y = tmp$_1; y <= tmp$_2; y++) {
          tmp$_3 = Kotlin.imul(i, this.twistBoxSize) + 1 | 0;
          tmp$_4 = Kotlin.imul(i + 1 | 0, this.twistBoxSize);
          for (var x = tmp$_3; x <= tmp$_4; x++) {
            cells.add_11rb$(grid.get_za3lpa$(y - 1 | 0).get_za3lpa$(x - 1 | 0));
          }
        }
        var wordId = 1001 + Kotlin.imul(j, this.width / this.twistBoxSize | 0) + i | 0;
        twistsCluesList.add_11rb$(new Clue(new Word(wordId, cells), (twistNumber + 1 | 0).toString(), this.twistsClues.get_za3lpa$(twistNumber)));
        twistNumber = twistNumber + 1 | 0;
      }
    }
    return twistsCluesList;
  };
  function TwistsAndTurns$Companion() {
    TwistsAndTurns$Companion_instance = this;
  }
  TwistsAndTurns$Companion.prototype.fromRawInput = function (title, creator, copyright, description, width, height, twistBoxSize, turnsAnswers, turnsClues, twistsClues, lightTwistsColor, darkTwistsColor, crosswordSolverSettings) {
    var tmp$ = toInt(width);
    var tmp$_0 = toInt(height);
    var tmp$_1 = toInt(twistBoxSize);
    var tmp$_2 = replace(turnsAnswers.toUpperCase(), '[^A-Z ]', '');
    return new TwistsAndTurns(title, creator, copyright, description, tmp$, tmp$_0, tmp$_1, Regex_init(' +').split_905azu$(tmp$_2, 0), split(turnsClues, ['\n']), split(twistsClues, ['\n']), lightTwistsColor, darkTwistsColor, crosswordSolverSettings);
  };
  TwistsAndTurns$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var TwistsAndTurns$Companion_instance = null;
  function TwistsAndTurns$Companion_getInstance() {
    if (TwistsAndTurns$Companion_instance === null) {
      new TwistsAndTurns$Companion();
    }return TwistsAndTurns$Companion_instance;
  }
  TwistsAndTurns.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'TwistsAndTurns',
    interfaces: []
  };
  TwistsAndTurns.prototype.component1 = function () {
    return this.title;
  };
  TwistsAndTurns.prototype.component2 = function () {
    return this.creator;
  };
  TwistsAndTurns.prototype.component3 = function () {
    return this.copyright;
  };
  TwistsAndTurns.prototype.component4 = function () {
    return this.description;
  };
  TwistsAndTurns.prototype.component5 = function () {
    return this.width;
  };
  TwistsAndTurns.prototype.component6 = function () {
    return this.height;
  };
  TwistsAndTurns.prototype.component7 = function () {
    return this.twistBoxSize;
  };
  TwistsAndTurns.prototype.component8 = function () {
    return this.turnsAnswers;
  };
  TwistsAndTurns.prototype.component9 = function () {
    return this.turnsClues;
  };
  TwistsAndTurns.prototype.component10 = function () {
    return this.twistsClues;
  };
  TwistsAndTurns.prototype.component11 = function () {
    return this.lightTwistsColor;
  };
  TwistsAndTurns.prototype.component12 = function () {
    return this.darkTwistsColor;
  };
  TwistsAndTurns.prototype.component13 = function () {
    return this.crosswordSolverSettings;
  };
  TwistsAndTurns.prototype.copy_68y921$ = function (title, creator, copyright, description, width, height, twistBoxSize, turnsAnswers, turnsClues, twistsClues, lightTwistsColor, darkTwistsColor, crosswordSolverSettings) {
    return new TwistsAndTurns(title === void 0 ? this.title : title, creator === void 0 ? this.creator : creator, copyright === void 0 ? this.copyright : copyright, description === void 0 ? this.description : description, width === void 0 ? this.width : width, height === void 0 ? this.height : height, twistBoxSize === void 0 ? this.twistBoxSize : twistBoxSize, turnsAnswers === void 0 ? this.turnsAnswers : turnsAnswers, turnsClues === void 0 ? this.turnsClues : turnsClues, twistsClues === void 0 ? this.twistsClues : twistsClues, lightTwistsColor === void 0 ? this.lightTwistsColor : lightTwistsColor, darkTwistsColor === void 0 ? this.darkTwistsColor : darkTwistsColor, crosswordSolverSettings === void 0 ? this.crosswordSolverSettings : crosswordSolverSettings);
  };
  TwistsAndTurns.prototype.toString = function () {
    return 'TwistsAndTurns(title=' + Kotlin.toString(this.title) + (', creator=' + Kotlin.toString(this.creator)) + (', copyright=' + Kotlin.toString(this.copyright)) + (', description=' + Kotlin.toString(this.description)) + (', width=' + Kotlin.toString(this.width)) + (', height=' + Kotlin.toString(this.height)) + (', twistBoxSize=' + Kotlin.toString(this.twistBoxSize)) + (', turnsAnswers=' + Kotlin.toString(this.turnsAnswers)) + (', turnsClues=' + Kotlin.toString(this.turnsClues)) + (', twistsClues=' + Kotlin.toString(this.twistsClues)) + (', lightTwistsColor=' + Kotlin.toString(this.lightTwistsColor)) + (', darkTwistsColor=' + Kotlin.toString(this.darkTwistsColor)) + (', crosswordSolverSettings=' + Kotlin.toString(this.crosswordSolverSettings)) + ')';
  };
  TwistsAndTurns.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.title) | 0;
    result = result * 31 + Kotlin.hashCode(this.creator) | 0;
    result = result * 31 + Kotlin.hashCode(this.copyright) | 0;
    result = result * 31 + Kotlin.hashCode(this.description) | 0;
    result = result * 31 + Kotlin.hashCode(this.width) | 0;
    result = result * 31 + Kotlin.hashCode(this.height) | 0;
    result = result * 31 + Kotlin.hashCode(this.twistBoxSize) | 0;
    result = result * 31 + Kotlin.hashCode(this.turnsAnswers) | 0;
    result = result * 31 + Kotlin.hashCode(this.turnsClues) | 0;
    result = result * 31 + Kotlin.hashCode(this.twistsClues) | 0;
    result = result * 31 + Kotlin.hashCode(this.lightTwistsColor) | 0;
    result = result * 31 + Kotlin.hashCode(this.darkTwistsColor) | 0;
    result = result * 31 + Kotlin.hashCode(this.crosswordSolverSettings) | 0;
    return result;
  };
  TwistsAndTurns.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.title, other.title) && Kotlin.equals(this.creator, other.creator) && Kotlin.equals(this.copyright, other.copyright) && Kotlin.equals(this.description, other.description) && Kotlin.equals(this.width, other.width) && Kotlin.equals(this.height, other.height) && Kotlin.equals(this.twistBoxSize, other.twistBoxSize) && Kotlin.equals(this.turnsAnswers, other.turnsAnswers) && Kotlin.equals(this.turnsClues, other.turnsClues) && Kotlin.equals(this.twistsClues, other.twistsClues) && Kotlin.equals(this.lightTwistsColor, other.lightTwistsColor) && Kotlin.equals(this.darkTwistsColor, other.darkTwistsColor) && Kotlin.equals(this.crosswordSolverSettings, other.crosswordSolverSettings)))));
  };
  Object.defineProperty(ZipOutputType, 'BASE64', {
    get: ZipOutputType$BASE64_getInstance
  });
  var package$com = _.com || (_.com = {});
  var package$jeffpdavidson = package$com.jeffpdavidson || (package$com.jeffpdavidson = {});
  var package$kotwords = package$jeffpdavidson.kotwords || (package$jeffpdavidson.kotwords = {});
  var package$jslib = package$kotwords.jslib || (package$kotwords.jslib = {});
  package$jslib.ZipOutputType = ZipOutputType;
  Object.defineProperty(ZipOutputCompression, 'DEFLATE', {
    get: ZipOutputCompression$DEFLATE_getInstance
  });
  package$jslib.ZipOutputCompression = ZipOutputCompression;
  package$jslib.newGenerateAsyncOptions_9i3lf0$ = newGenerateAsyncOptions;
  var package$model = package$kotwords.model || (package$kotwords.model = {});
  package$model.CrosswordSolverSettings = CrosswordSolverSettings;
  package$model.Cell = Cell;
  package$model.Word = Word;
  package$model.Clue = Clue;
  package$model.ClueList = ClueList;
  package$model.Jpz = Jpz;
  Object.defineProperty(TwistsAndTurns, 'Companion', {
    get: TwistsAndTurns$Companion_getInstance
  });
  package$model.TwistsAndTurns = TwistsAndTurns;
  Kotlin.defineModule('kotwords', _);
  return _;
}));

//# sourceMappingURL=kotwords.js.map
