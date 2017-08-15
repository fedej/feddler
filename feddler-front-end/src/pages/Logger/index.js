import React, { Component } from 'react';
import Websocket from 'react-websocket';
import AppBar from 'material-ui/AppBar';
import TextField from 'material-ui/TextField';
import RaisedButton from 'material-ui/RaisedButton';
import {
  Table,
  TableBody,
  TableHeader,
  TableHeaderColumn,
  TableRow,
  TableRowColumn,
  TableFooter,
} from 'material-ui/Table';
import moment from 'moment';
import logo from '../../logo.svg';
import '../../App.css';
import ExchangeDialog from '../../components/ExchangeDialog';
import PagiFooter from '../../components/PagiFooter';

class Logger extends Component {
  state = {
    selected: false,
    values: [],
    selectedExchange: {},
    tempHost: 'localhost:8083',
    offset: 0,
    limit: 5,
  };

  handleData = (data) => {
    const { values } = this.state;
    const parsed = JSON.parse(data);
    parsed.time = moment().format('DD/MM/YYYY HH:mm:ss');
    values.unshift(parsed);
    this.setState({ values });
  }

  pagiFunction = (offset) => {
    this.setState({ offset });
  }

  render() {
    const values = this.state.values ? this.state.values : [];
    const { offset, limit } = this.state;
    const total = values ? values.length : 0;

    return (
      <div className="App">
        <AppBar />
        <br />
        <div style={{ display: 'flex', alignItems: 'left', justifyContent: 'space-around' }}>
          <TextField
            hintText="host:port"
            value={this.state.tempHost}
            onChange={(e, tempHost) => this.setState({ tempHost })}
          />
          <RaisedButton
            label="Connect"
            onTouchTap={() => this.setState({ host: this.state.tempHost })}
            primary
          />
          <RaisedButton
            label="Disconnect"
            onTouchTap={() => this.setState({ host: null })}
          />
        </div>
        {this.state.host ? (
          <Websocket url={`ws://${this.state.host}/echo`} onMessage={this.handleData} />
        ) : null}
        <div className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
          <h2>Welcome to React</h2>
        </div>
        <ExchangeDialog
          exchange={this.state.selectedExchange}
          open={this.state.selected}
        />
        <Table>
          <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
            <TableRow>
              <TableHeaderColumn>Time</TableHeaderColumn>
              <TableHeaderColumn>Method</TableHeaderColumn>
              <TableHeaderColumn>Path</TableHeaderColumn>
              <TableHeaderColumn>Request</TableHeaderColumn>
              <TableHeaderColumn>Response</TableHeaderColumn>
            </TableRow>
          </TableHeader>
          {values ? (
            <TableBody displayRowCheckbox={false}>
              {
                values.slice(offset, offset + limit).map((p, i) =>
                  (<TableRow>
                    <TableRowColumn
                      style={{ whiteSpace: 'normal', wordWrap: 'break-word' }}
                    >
                      {p.time}
                    </TableRowColumn>
                    <TableRowColumn
                      style={{ whiteSpace: 'normal', wordWrap: 'break-word' }}
                    >
                      {p.request.method}
                    </TableRowColumn>
                    <TableRowColumn
                      style={{ whiteSpace: 'normal', wordWrap: 'break-word' }}
                    >
                      {p.request.uri}
                    </TableRowColumn>
                    <TableRowColumn>
                      <RaisedButton
                        label="View"
                        onTouchTap={() => {
                          const selectedExchange = this.state.values[i].request;
                          this.setState({ selectedExchange, selected: true });
                        }}
                        primary
                      />
                    </TableRowColumn>
                    <TableRowColumn>
                      <RaisedButton
                        label="View"
                        onTouchTap={() => {
                          const selectedExchange = this.state.values[i].response;
                          this.setState({ selectedExchange, selected: true });
                        }}
                        primary
                      />
                    </TableRowColumn>
                  </TableRow>),
                )
              }
            </TableBody>
          ) : null}
          <TableFooter>
            <TableRow>
              <TableRowColumn colSpan="3">
                <PagiFooter
                  offset={offset}
                  total={total}
                  limit={limit}
                  onPageClick={this.pagiFunction}
                />
              </TableRowColumn>
            </TableRow>
          </TableFooter>
        </Table>
      </div>
    );
  }
}

export default Logger;
