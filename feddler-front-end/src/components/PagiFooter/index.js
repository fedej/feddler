import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ChevronLeft from 'material-ui/svg-icons/navigation/chevron-left';
import ChevronRight from 'material-ui/svg-icons/navigation/chevron-right';
import IconButton from 'material-ui/IconButton';

const styles = {
  footerContent: {
    float: 'right',
  },
  footerText: {
    float: 'right',
    paddingTop: '16px',
    height: '16px',
  },
};

export default class PagiFooter extends Component {
  static propTypes = {
    offset: PropTypes.number.isRequired, // current offset
    total: PropTypes.number.isRequired, // total number of rows
    limit: PropTypes.number.isRequired, // num of rows in each page
    onPageClick: PropTypes.func.isRequired, // what to do after clicking page number
  }

  state = {
    offset: 0,
    total: 0,
    limit: 0,
  };

  componentWillMount() {
    const { offset, limit, total } = this.props;
    this.setState({ offset, limit, total });
  }

  componentWillReceiveProps(nextProps) {
    const { offset, limit, total } = nextProps;
    this.setState({ offset, limit, total });
  }

  render() {
    const { offset, total, limit } = this.state;

    return (
      <div style={styles.footerContent}>
        <IconButton
          disabled={offset === 0}
          onClick={() => this.props.onPageClick(offset - limit)}
        >
          <ChevronLeft />
        </IconButton>
        <IconButton
          disabled={offset + limit >= total}
          onClick={() => this.props.onPageClick(offset + limit)}
        >
          <ChevronRight />
        </IconButton>
        {`${Math.min((offset + 1), total)}-${Math.min((offset + limit), total)} of ${total}`}
      </div>
    );
  }
}
