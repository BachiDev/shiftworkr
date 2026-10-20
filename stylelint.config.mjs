export default {
  rules: {
    // IntelliJ's CssInvalidHtmlTagReference equivalent: catches typos and
    // light-DOM selectors for web components. Use ::part() or a custom
    // property, or suppress the single rule with a stylelint comment.
    "selector-type-no-unknown": true
  }
};
