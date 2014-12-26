var VersionPicker = function() {
  var self = {
    akkaVersion: function () {
      return $('#akka-version').innerText;
    },

    init: function () {
      var staticVersion = $('#version-static');

      var picker = $('#version-picker');
      var versions = picker.find('li');

      var combo = $('<select></select>').attr('id', 'version-combo').attr('name', 'version-combo');
      $.each(versions, function (i, el) {
        combo.append('<option value="' + el.innerText + '">' + el.innerText + '</option>');
      });

      combo.change(function () {
        const targetUrl = window.location.href.replace(self.akkaVersion(), combo.val());
        console.log("Picked", combo.val(), "redirecting to:", targetUrl);
        window.location.replace(targetUrl);
      });

      staticVersion.hide();
      picker.replaceWith(combo);
    }
  };

  return self;
}();

var LangPicker = function () {
  var self = {
    init: function() {
      var staticLang = $('#api-lang-static');

      var picker = $('#api-lang-picker');
      var versions = picker.find('li');

      var combo = $('<select></select>').attr('id', 'api-lang-combo').attr('name', 'api-lang-combo');
      $.each(versions, function (i, el) {
        combo.append('<option value="' + el.innerText + '">' + el.innerText + '</option>');
      });

      combo.change(function () {
        const targetUrl = window.location.href.replace($('#this-api-lang'), combo.val());
        console.log("Picked", combo.val(), "redirecting to:", targetUrl);
        window.location.replace(targetUrl);
      });

      staticLang.hide();
      picker.replaceWith(combo).prepend('<span> for </span>');
    }
  };

  return self;
}();

$(document).ready(VersionPicker.init);
$(document).ready(LangPicker.init);