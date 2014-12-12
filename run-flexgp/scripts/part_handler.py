
def list_types():
    # return a list of mime-types that are handled by this module
    return(["text/plain"])

def handle_part(data, ctype, filename, payload):
    # data: the cloudinit object
    # ctype: '__begin__', '__end__', or the specific mime-type of the part
    # filename: the filename for the part, or dynamically generated part if
    #           no filename is given attribute is present
    # payload: the content of the part (empty for begin or end)
    if ctype == "__begin__":
       print "my handler is beginning"
       return
    if ctype == "__end__":
       print "my handler is ending"
       return

    print "==== received ctype=%s filename=%s ====" % (ctype,filename)
    import os
    new_file = "/home/ubuntu/%s" % (filename)
    lines = payload.splitlines(True)
    print lines
    print " == Creating file %s" % (new_file)
    with open(new_file,'wb') as f:
        for line in lines:
            f.write(line)
    print "==== end ctype=%s filename=%s" % (ctype, filename)
