package com.maddyhome.idea.vim.handler;

/*
* IdeaVim - A Vim emulator plugin for IntelliJ Idea
* Copyright (C) 2003 Rick Maddy
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.undo.UndoManager;

/**
 *
 */
public abstract class VisualOperatorActionHandler extends AbstractEditorActionHandler
{
    protected final boolean execute(final Editor editor, DataContext context, Command cmd)
    {
        logger.debug("execute, cmd=" + cmd);
        if (cmd == null || !cmd.isReadType())
        {
            UndoManager.getInstance().endCommand(editor);
            UndoManager.getInstance().beginCommand(editor);
        }
        if (CommandState.getInstance().getMode() == CommandState.MODE_REPEAT)
        {
            CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 1, 0);
        }

        TextRange range = null;
        VisualChange change = null;
        if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
        {
            range = CommandGroups.getInstance().getMotion().getVisualRange(editor);
            change = CommandGroups.getInstance().getMotion().getVisualOperatorRange(editor,
                cmd == null ? Command.FLAG_MOT_LINEWISE : cmd.getFlags());
            logger.debug("range=" + range);
            logger.debug("change=" + change);
        }

        // If this is a mutli key change then exit visual now
        if (cmd != null && (cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) != 0)
        {
            logger.debug("multikey undo - exit visual");
            CommandGroups.getInstance().getMotion().exitVisual(editor);
        }

        boolean res = execute(editor, context, cmd, range);

        // TODO - all the following code needs to be executed AFTER the command really finishes.
        // As is it seems to work for all IDEA commands except Surround With Live Template. This
        // code gets called well before the user even has a chance to select the template so by
        // the time the Surround command runs, the selection is gone and this causes a problem.
        // Need some way to delay this code until the change actually occurs - DocumentListener?

        // If this was a "simple" command then exit visual now
        if (cmd == null || (cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0)
        {
            logger.debug("not multikey undo - exit visual");
            CommandGroups.getInstance().getMotion().exitVisual(editor);
        }

        if (res)
        {
            logger.debug("res");
            if (change != null)
            {
                EditorData.setLastVisualOperatorRange(editor, change);
            }

            if (cmd == null || ((cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0 && !cmd.isReadType()))
            {
                UndoManager.getInstance().endCommand(editor);
                UndoManager.getInstance().beginCommand(editor);
            }
            if (cmd != null)
            {
                CommandState.getInstance().saveLastChangeCommand(cmd);
            }
        }
        else
        {
            if (cmd == null || !cmd.isReadType())
            {
                UndoManager.getInstance().abortCommand(editor);
                UndoManager.getInstance().beginCommand(editor);
            }
        }

        return res;
    }

    protected abstract boolean execute(Editor editor, DataContext context, Command cmd, TextRange range);

    private static Logger logger = Logger.getInstance(VisualOperatorActionHandler.class.getName());
}
