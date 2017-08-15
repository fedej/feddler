import React from 'react';
import PropTypes from 'prop-types';
import Dialog from 'material-ui/Dialog';
import FlatButton from 'material-ui/FlatButton';
import {
  Table,
  TableBody,
  TableHeader,
  TableHeaderColumn,
  TableRow,
  TableRowColumn,
} from 'material-ui/Table';
import Chip from 'material-ui/Chip';
import ReactJson from 'react-json-view';

export default class ExchangeDialog extends React.Component {
  static propTypes = {
    open: PropTypes.bool.isRequired,
    exchange: PropTypes.shape({}).isRequired,
  }

  state = {
    open: false,
    exchange: {},
  };

  componentWillMount() {
    const { open, exchange } = this.props;
    this.setState({ open, exchange });
  }

  componentWillReceiveProps(nextProps) {
    const { open, exchange } = nextProps;
    this.setState({ open, exchange });
  }

  handleOpen = () => {
    this.setState({ open: true });
  };

  handleClose = () => {
    this.setState({ open: false });
  };

  render() {
    const exchange = this.state.exchange;
    const actions = [
      <FlatButton
        label="Close"
        primary
        keyboardFocused
        onTouchTap={this.handleClose}
      />,
    ];

    return (
      <div>
        <Dialog
          title="Exchange Details"
          actions={actions}
          modal={false}
          open={this.state.open}
          onRequestClose={this.handleClose}
          contentStyle={{ width: '100%', maxWidth: 'none' }}
          autoScrollBodyContent
        >
          <Table style={{ tableLayout: 'auto' }}>
            <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
              <TableRow>
                <TableHeaderColumn>Name</TableHeaderColumn>
                <TableHeaderColumn>Value</TableHeaderColumn>
              </TableRow>
            </TableHeader>
            {exchange && exchange ? (
              <TableBody displayRowCheckbox={false}>
                {
                  Object.keys(exchange).map(key =>
                    (<TableRow>
                      <TableRowColumn>{key.toString()}</TableRowColumn>
                      <TableRowColumn>
                        {key === 'headers' ?
                          exchange[key].map(p => <Chip>{p}</Chip>)
                          : null
                        }
                        {key === 'body' && !(key === 'headers') ?
                          <ReactJson name={false} collapsed src={JSON.parse(exchange[key])} />
                          : JSON.stringify(exchange[key])
                        }
                      </TableRowColumn>
                    </TableRow>),
                  )
                }
              </TableBody>
            ) : null
            }
          </Table>
        </Dialog>
      </div>
    );
  }
}
