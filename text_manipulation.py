def get_text_separated(token):
    if not hasattr(token, "getChildren") or not token.getChildren():
        return token.getText()
    return " ".join([get_text_separated(child) for child in token.getChildren()])
